import { PartialType } from '@nestjs/swagger';
import { CreateSubscriptionOptionDto } from './create-subscription-option.dto';

export class UpdateSubscriptionOptionDto extends PartialType(CreateSubscriptionOptionDto) { }
