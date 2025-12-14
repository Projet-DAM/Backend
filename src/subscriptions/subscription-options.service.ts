import { Injectable } from '@nestjs/common';
import { SubscriptionOptionType } from '../subscriptions/schemas/subscription-option.schema';

export interface AvailableOption {
    type: SubscriptionOptionType;
    name: string;
    description: string;
    price: number;
    currency: string;
}

@Injectable()
export class SubscriptionOptionsService {
    /**
     * Get all available subscription options
     */
    getAvailableOptions(): AvailableOption[] {
        return [
            {
                type: SubscriptionOptionType.SPORTS_OUTFIT,
                name: 'Tenue sportive',
                description: 'Tenue sportive complète (maillot, short, chaussettes)',
                price: 50,
                currency: 'TND',
            },
            {
                type: SubscriptionOptionType.INSURANCE,
                name: 'Assurance',
                description: 'Assurance accident et responsabilité civile',
                price: 30,
                currency: 'TND',
            },
            {
                type: SubscriptionOptionType.TRANSPORT,
                name: 'Transport',
                description: 'Service de transport aller-retour',
                price: 40,
                currency: 'TND',
            },
        ];
    }

    /**
     * Get a specific option by type
     */
    getOptionByType(type: SubscriptionOptionType): AvailableOption | undefined {
        return this.getAvailableOptions().find(opt => opt.type === type);
    }

    /**
     * Calculate total price for selected options
     */
    calculateOptionsTotal(selectedTypes: SubscriptionOptionType[]): number {
        const availableOptions = this.getAvailableOptions();
        return selectedTypes.reduce((total, type) => {
            const option = availableOptions.find(opt => opt.type === type);
            return total + (option?.price || 0);
        }, 0);
    }
}
