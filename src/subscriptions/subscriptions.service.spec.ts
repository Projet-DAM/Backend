import { Test } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { SubscriptionsService } from './subscriptions.service';
import { Subscription } from './schemas/subscription.schema';
import { UsersService } from '../users/users.service';
import { OffersService } from '../offers/offers.service';
import { UserRole } from '../users/interfaces/user-role.enum';

function createModelMock() {
  const data: any[] = [];
  return {
    data,
    create: jest.fn(async (doc: any) => {
      const instance = { ...doc, _id: `${data.length + 1}`, save: async () => instance };
      data.push(instance);
      return instance;
    }),
    findById: jest.fn(async (id: string) => {
      const found = data.find((d) => d._id === id);
      return found ? { ...found, save: async () => found } : null;
    }),
    find: jest.fn(() => ({ sort: () => ({ skip: () => ({ limit: () => ({ exec: async () => data }) }) }) })),
    countDocuments: jest.fn(async () => data.length),
  } as any;
}

describe('SubscriptionsService', () => {
  let service: SubscriptionsService;
  let model: any;

  beforeEach(async () => {
    model = createModelMock();
    const moduleRef = await Test.createTestingModule({
      providers: [
        SubscriptionsService,
        { provide: getModelToken(Subscription.name), useValue: model },
        { provide: UsersService, useValue: { findById: jest.fn(async (id: string) => id === 'child1' ? { _id: 'child1', parent: 'parent1' } : null) } },
        { provide: OffersService, useValue: { findOne: jest.fn(async (id: string) => ({ _id: id, durationDays: 30, price: 100, discountPct: 0, isActive: true })) } },
      ],
    }).compile();
    service = moduleRef.get(SubscriptionsService);
  });

  it('should create with computed endDate and PENDING/UNPAID', async () => {
    const start = new Date();
    const created = await service.create({ childId: 'child1', offerId: 'offer1', startDate: start.toISOString(), autoRenew: true }, { userId: 'parent1', role: UserRole.PARENT });
    expect(created.status).toBe('PENDING');
    expect(created.paymentStatus).toBe('UNPAID');
    const diffDays = Math.round((new Date(created.endDate).getTime() - new Date(created.startDate).getTime()) / 86400000) + 1;
    expect(diffDays).toBe(30);
  });

  it('should record payment and set PAID and ACTIVE', async () => {
    const created = await service.create({ childId: 'child1', offerId: 'offer1', autoRenew: true }, { userId: 'parent1', role: UserRole.PARENT });
    const updated = await service.recordPayment(created._id.toString(), { amount: 100, currency: 'EUR', method: 'CASH' }, { userId: 'parent1', role: UserRole.PARENT });
    expect(updated.paymentStatus).toBe('PAID');
    expect(updated.status).toBe('ACTIVE');
  });
});






