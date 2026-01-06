import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument, SubscriptionStatus, PaymentStatus } from '../subscriptions/schemas/subscription.schema';

export interface MonthlyRevenue {
    month: string;
    label: string;
    revenue: number;
    subscriptionCount: number;
}

export interface DistributionPoint {
    offerName: string;
    revenue: number;
    count: number;
    percentage: number;
}

export interface RevenueForecast {
    history: MonthlyRevenue[];
    forecast: MonthlyRevenue[];
    stats: {
        totalRevenue: number;
        avgMonthlyRevenue: number;
        arpu: number; // Average Revenue Per User
        growthPercentage: number;
        topOffer: string;
    };
    explanation: string;
    trend: string; // 'croissante', 'décroissante', 'stable'
    distribution: DistributionPoint[];
}

@Injectable()
export class ForecastService {
    private readonly logger = new Logger(ForecastService.name);

    constructor(
        @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    ) { }

    /**
     * Génère des prévisions de revenus basées sur les données historiques
     */
    async generateRevenueForecast(academyId?: string): Promise<RevenueForecast> {
        try {
            this.logger.log(`📊 Dashboard Financier pour academyId: ${academyId || 'Tous'}`);

            const subscriptions = await this.subscriptionModel
                .find({ status: { $in: [SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED, SubscriptionStatus.PENDING] }, paymentStatus: PaymentStatus.PAID })
                .populate('offerId')
                .lean()
                .exec();

            const filteredSubs = academyId
                ? subscriptions.filter(sub => (sub.offerId as any)?.academyId?.toString() === academyId)
                : subscriptions;

            // 1. Calculer l'historique mensuel
            const history = this.calculateMonthlyRevenue(filteredSubs);

            // 2. Calculer la répartition détaillée par offre
            const distributionMap = new Map<string, { revenue: number, count: number }>();
            let totalRevenueSum = 0;

            for (const sub of filteredSubs) {
                const offer = sub.offerId as any;
                if (!offer) continue;
                const name = offer.name || 'Inconnue';
                let rev = offer.price * (1 - (offer.discountPct || 0) / 100);
                if (sub.selectedOptions) {
                    rev += sub.selectedOptions.reduce((acc: number, opt: any) => acc + (Number(opt.price) || 0), 0);
                }

                if (!distributionMap.has(name)) {
                    distributionMap.set(name, { revenue: 0, count: 0 });
                }
                const current = distributionMap.get(name)!;
                current.revenue += rev;
                current.count += 1;
                totalRevenueSum += rev;
            }

            const distribution: DistributionPoint[] = Array.from(distributionMap.entries())
                .map(([offerName, data]) => ({
                    offerName,
                    revenue: Math.round(data.revenue * 100) / 100,
                    count: data.count,
                    percentage: totalRevenueSum > 0 ? Math.round((data.revenue / totalRevenueSum) * 100) : 0
                }))
                .sort((a, b) => b.revenue - a.revenue);

            // 3. Calcul de la tendance et KPIs
            const rawTrend = this.calculateTrend(history);
            const trend = rawTrend === 'increasing' ? 'croissante' : rawTrend === 'decreasing' ? 'décroissante' : 'stable';

            const prediction = this.predictNextMonth(history);
            const nextMonthDate = new Date();
            const monthNames = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sept', 'Oct', 'Nov', 'Déc'];
            nextMonthDate.setMonth(nextMonthDate.getMonth() + 1);

            const forecast: MonthlyRevenue[] = [
                {
                    month: nextMonthDate.toISOString().slice(0, 7),
                    label: `${monthNames[nextMonthDate.getMonth()]} ${nextMonthDate.getFullYear()}`,
                    revenue: prediction.estimatedRevenue,
                    subscriptionCount: prediction.estimatedSubscriptions
                }
            ];

            const currentMonthData = history[history.length - 1];
            const prevMonthData = history.length >= 2 ? history[history.length - 2] : null;
            const growthPercentage = prevMonthData ? Math.round(((currentMonthData.revenue / prevMonthData.revenue) - 1) * 100) : 0;

            // 4. Statistiques de haut niveau (Finance Header)
            const stats = {
                totalRevenue: Math.round(totalRevenueSum),
                avgMonthlyRevenue: history.length > 0 ? Math.round(totalRevenueSum / history.length) : 0,
                arpu: filteredSubs.length > 0 ? Math.round((totalRevenueSum / filteredSubs.length) * 10) / 10 : 0,
                growthPercentage,
                topOffer: distribution.length > 0 ? distribution[0].offerName : 'N/A'
            };

            // 5. Explication Professionnelle
            let explanation = `Votre académie affiche une santé financière **${trend}**. `;
            if (growthPercentage > 0) {
                explanation += `La croissance de **${growthPercentage}%** ce mois-ci est portée principalement par l'offre **${stats.topOffer}**. `;
            }
            explanation += `Le revenu moyen par élève est de **${stats.arpu} TND**. `;
            explanation += `Pour le mois prochain, nous anticipons une stabilisation autour de **${prediction.estimatedRevenue} TND**.`;

            return {
                history,
                forecast,
                stats,
                explanation,
                trend,
                distribution
            };
        } catch (error) {
            this.logger.error('❌ Erreur dashboard financier:', error);
            throw error;
        }
    }

    private calculateMonthlyRevenue(subscriptions: any[]): MonthlyRevenue[] {
        const monthlyMap = new Map<string, { revenue: number; count: number }>();
        const monthNames = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sept', 'Oct', 'Nov', 'Déc'];

        for (const sub of subscriptions) {
            try {
                const date = sub.createdAt ? new Date(sub.createdAt) : (sub.startDate ? new Date(sub.startDate) : new Date());
                if (isNaN(date.getTime())) continue;

                const monthKey = date.toISOString().slice(0, 7); // YYYY-MM
                const offer = sub.offerId as any;

                if (!offer || typeof offer.price !== 'number') continue;

                let revenue = offer.price * (1 - (offer.discountPct || 0) / 100);
                if (sub.selectedOptions && Array.isArray(sub.selectedOptions)) {
                    const optionsTotal = sub.selectedOptions.reduce((acc: number, opt: any) => acc + (Number(opt.price) || 0), 0);
                    revenue += optionsTotal;
                }

                if (!monthlyMap.has(monthKey)) {
                    monthlyMap.set(monthKey, { revenue: 0, count: 0 });
                }

                const data = monthlyMap.get(monthKey)!;
                data.revenue += revenue;
                data.count += 1;
            } catch (err) { }
        }

        // Convertir en tableau et trier par mois
        return Array.from(monthlyMap.entries())
            .map(([monthKey, data]) => {
                const [year, month] = monthKey.split('-');
                const monthIdx = parseInt(month, 10) - 1;
                return {
                    month: monthKey,
                    label: `${monthNames[monthIdx]} ${year}`, // ex: "Déc 2025"
                    revenue: Math.round(data.revenue * 100) / 100,
                    value: Math.round(data.revenue * 100) / 100, // Alias souvent utilisé par les graphiques
                    subscriptionCount: data.count,
                };
            })
            .sort((a, b) => a.month.localeCompare(b.month));
    }

    private calculateTrend(monthlyData: MonthlyRevenue[]): 'increasing' | 'stable' | 'decreasing' {
        if (monthlyData.length < 2) return 'stable';

        const recentMonths = monthlyData.slice(-3);
        const revenues = recentMonths.map(m => m.revenue);

        // Calculer la tendance simple
        let increasing = 0;
        let decreasing = 0;

        for (let i = 1; i < revenues.length; i++) {
            if (revenues[i] > revenues[i - 1]) increasing++;
            else if (revenues[i] < revenues[i - 1]) decreasing++;
        }

        if (increasing > decreasing) return 'increasing';
        if (decreasing > increasing) return 'decreasing';
        return 'stable';
    }

    private predictNextMonth(monthlyData: MonthlyRevenue[]): {
        estimatedRevenue: number;
        estimatedSubscriptions: number;
        confidence: number;
    } {
        if (monthlyData.length === 0) {
            return { estimatedRevenue: 0, estimatedSubscriptions: 0, confidence: 0 };
        }

        // 1. Calcul de la croissance moyenne (MoM)
        let growthTotal = 0;
        let growthCount = 0;
        for (let i = 1; i < monthlyData.length; i++) {
            if (monthlyData[i - 1].revenue > 0) {
                growthTotal += (monthlyData[i].revenue / monthlyData[i - 1].revenue);
                growthCount++;
            }
        }
        const avgGrowth = growthCount > 0 ? (growthTotal / growthCount) : 1;

        // 2. Calcul par Moyenne Pondérée (Donne du poids au récent)
        const recentMonths = monthlyData.slice(-3);
        const weights = recentMonths.length === 3 ? [0.15, 0.25, 0.60] : [0.4, 0.6];

        let weightedRevenue = 0;
        let weightedSubs = 0;
        let totalWeight = 0;

        recentMonths.forEach((m, i) => {
            const w = weights[i] || 1 / recentMonths.length;
            weightedRevenue += (m.revenue || 0) * w;
            weightedSubs += (m.subscriptionCount || 0) * w;
            totalWeight += w;
        });

        // 3. Synthèse : On applique la tendance de croissance à la moyenne pondérée
        // Si la croissance est trop brusque (>30%), on la tempère pour être réaliste
        const realisticGrowth = Math.min(1.3, Math.max(0.7, avgGrowth));
        const estimatedRevenue = (weightedRevenue / totalWeight) * realisticGrowth;
        const estimatedSubscriptions = (weightedSubs / totalWeight) * realisticGrowth;

        // 4. Calcul de l'indice de confiance (Stabilité des revenus)
        const revenues = monthlyData.map(m => m.revenue);
        const stdev = this.calculateVariance(revenues);
        const mean = revenues.reduce((a, b) => a + b, 0) / revenues.length;
        const cv = mean > 0 ? (stdev / mean) : 1; // Coefficient de variation

        // Plus le CV est bas, plus les revenus sont stables, plus la confiance est haute
        const confidence = Math.max(10, Math.min(95, 100 - (cv * 100)));

        return {
            estimatedRevenue: Math.round(estimatedRevenue * 100) / 100,
            estimatedSubscriptions: Math.round(estimatedSubscriptions),
            confidence: Math.round(confidence),
        };
    }

    private calculateVariance(values: number[]): number {
        if (values.length === 0) return 0;
        const mean = values.reduce((sum, val) => sum + val, 0) / values.length;
        const squaredDiffs = values.map(val => Math.pow(val - mean, 2));
        const variance = squaredDiffs.reduce((sum, val) => sum + val, 0) / values.length;
        return Math.sqrt(variance);
    }
}
