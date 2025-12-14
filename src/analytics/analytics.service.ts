import { Injectable, ForbiddenException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Subscription, SubscriptionDocument } from '../subscriptions/schemas/subscription.schema';
import { UserRole } from '../users/interfaces/user-role.enum';
import * as ss from 'simple-statistics';

@Injectable()
export class AnalyticsService {
    constructor(
        @InjectModel(Subscription.name) private subModel: Model<SubscriptionDocument>,
    ) { }

    async getRevenueForecast(actor: { userId: string; role: UserRole }, monthsToPredict: number = 6) {
        if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
            throw new ForbiddenException('Accès réservé aux administrateurs');
        }

        const history = await this.aggregateHistory(actor);
        const distribution = await this.getRevenueDistribution(actor);

        let regressionLine: (x: number) => number;
        let m = 0; // Slope
        let rSquared = 0;

        if (history.length === 0) {
            return {
                history,
                forecast: [],
                distribution,
                message: "Aucune donnée historique disponible pour la prédiction.",
                explanation: "Pas assez de données pour générer une prévision.",
                metrics: { rSquared: 0, sampleSize: 0 }
            };
        } else if (history.length === 1) {
            // Only 1 month of data: Project stable revenue
            const singleVal = history[0].revenue;
            regressionLine = (x) => singleVal;
            m = 0;
            rSquared = 1; // Technically perfect fit for 1 point but meaningless
        } else {
            // Standard Linear Regression (>= 2 points)
            const dataPoints = history.map((h, index) => [index, h.revenue]);
            const result = ss.linearRegression(dataPoints);
            m = result.m;
            regressionLine = ss.linearRegressionLine(result);
            try {
                rSquared = ss.rSquared(dataPoints, regressionLine);
            } catch (e) {
                // Fallback if calculation fails (e.g. variance is 0)
                rSquared = 0;
            }
        }

        const lastMonthIndex = history.length - 1;
        const lastHistoryDate = new Date(history[lastMonthIndex].date);

        const forecast: { month: string; revenue: number; isPredicted: boolean }[] = [];
        for (let i = 1; i <= monthsToPredict; i++) {
            const nextIndex = lastMonthIndex + i;

            let predictedRevenue = 0;
            if (history.length === 1) {
                predictedRevenue = history[0].revenue;
            } else {
                predictedRevenue = Math.max(0, regressionLine(nextIndex));
            }

            const nextDate = new Date(lastHistoryDate);
            nextDate.setMonth(nextDate.getMonth() + i);

            forecast.push({
                month: `${nextDate.getFullYear()}-${String(nextDate.getMonth() + 1).padStart(2, '0')}`,
                revenue: Math.round(predictedRevenue),
                isPredicted: true
            });
        }

        // Generate Detailed Explanation (French)
        let explanation = "";
        const trendText = m > 0 ? "croissante" : (m < 0 ? "décroissante" : "stable");
        const reliability = history.length < 3 ? "incertaine" : (rSquared > 0.6 ? "haute" : (rSquared > 0.3 ? "moyenne" : "faible"));

        explanation += `Tendance ${trendText}. `;
        if (m !== 0) {
            explanation += `Croissance estimée de ${Math.round(m)} TND/mois. `;
        } else {
            explanation += `Revenus stables. `;
        }

        explanation += `Fiabilité ${reliability} (basée sur ${history.length} mois).`;

        if (history.length < 3) {
            explanation += " Plus d'historique est nécessaire pour affiner la prédiction.";
        }

        return {
            history,
            forecast,
            distribution,
            trend: m > 0 ? 'croissante' : (m < 0 ? 'décroissante' : 'stable'),
            growthRate: m,
            explanation,
            metrics: {
                rSquared,
                sampleSize: history.length
            }
        };
    }

    private async aggregateHistory(actor: { userId: string; role: UserRole }) {
        const revenuePipeline: any[] = [
            { $unwind: '$transactions' },
            { $match: { 'transactions.status': 'SUCCESS' } },
        ];

        if (actor.role === UserRole.ACADEMIE) {
            let academyId: any = actor.userId;
            try {
                academyId = new Types.ObjectId(actor.userId);
            } catch (e) {
                // Keep as string if cast fails
            }

            revenuePipeline.unshift(
                {
                    $lookup: {
                        from: 'offers',
                        localField: 'offerId',
                        foreignField: '_id',
                        as: 'offer'
                    }
                },
                { $unwind: '$offer' },
                {
                    $match: {
                        $or: [
                            { 'offer.academyId': academyId },
                            { 'offer.academyId': actor.userId }
                        ]
                    }
                }
            );
        }

        revenuePipeline.push({
            $group: {
                _id: {
                    year: { $year: '$transactions.date' },
                    month: { $month: '$transactions.date' }
                },
                revenue: { $sum: '$transactions.amount' },
                transactionCount: { $sum: 1 }
            }
        });

        revenuePipeline.push({ $sort: { '_id.year': 1, '_id.month': 1 } });

        const results = await this.subModel.aggregate(revenuePipeline);

        return results.map(r => ({
            month: `${r._id.year}-${String(r._id.month).padStart(2, '0')}`,
            date: new Date(r._id.year, r._id.month - 1, 1),
            revenue: r.revenue,
            transactions: r.transactionCount
        }));
    }

    private async getRevenueDistribution(actor: { userId: string; role: UserRole }) {
        const pipeline: any[] = [
            { $unwind: '$transactions' },
            { $match: { 'transactions.status': 'SUCCESS' } },
        ];

        let academyId: any = actor.userId;
        if (actor.role === UserRole.ACADEMIE) {
            try {
                academyId = new Types.ObjectId(actor.userId);
            } catch (e) { }
        }

        // Always lookup offer to get title
        pipeline.push(
            {
                $lookup: {
                    from: 'offers',
                    localField: 'offerId',
                    foreignField: '_id',
                    as: 'offer'
                }
            },
            { $unwind: '$offer' }
        );

        if (actor.role === UserRole.ACADEMIE) {
            pipeline.push({
                $match: {
                    $or: [
                        { 'offer.academyId': academyId },
                        { 'offer.academyId': actor.userId }
                    ]
                }
            });
        }

        pipeline.push({
            $group: {
                _id: '$offer.title',
                revenue: { $sum: '$transactions.amount' },
                count: { $sum: 1 }
            }
        });

        pipeline.push({ $sort: { revenue: -1 } }); // Sort by highest revenue

        const results = await this.subModel.aggregate(pipeline);

        return results.map(r => ({
            name: r._id,
            revenue: r.revenue,
            percentage: 0 // Frontend can calculate or I can do it here if I sum total first. 
            // Simpler to just return raw revenue.
        }));
    }
}
