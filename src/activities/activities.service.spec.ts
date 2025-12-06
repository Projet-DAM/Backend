import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { ActivitiesService } from './activities.service';
import { Activity } from './schemas/activity.schema';
import { UserRole } from '../users/interfaces/user-role.enum';

describe('ActivitiesService', () => {
  let service: ActivitiesService;
  let model: any;

  beforeEach(async () => {
    const mockModel = {
      create: jest.fn(),
      find: jest.fn().mockReturnThis(),
      sort: jest.fn().mockReturnThis(),
      skip: jest.fn().mockReturnThis(),
      limit: jest.fn().mockReturnThis(),
      exec: jest.fn(),
      countDocuments: jest.fn().mockReturnThis(),
      findById: jest.fn().mockReturnThis(),
      findByIdAndUpdate: jest.fn().mockReturnThis(),
      findByIdAndDelete: jest.fn().mockReturnThis(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ActivitiesService,
        {
          provide: getModelToken(Activity.name),
          useValue: mockModel,
        },
      ],
    }).compile();

    service = module.get<ActivitiesService>(ActivitiesService);
    model = module.get(getModelToken(Activity.name));
  });

  it('should create an activity as coach', async () => {
    const dto = { nom_activite: 'Foot', date: '2025-11-15' } as any;
    const user = { role: UserRole.COACH, sub: '507f1f77bcf86cd799439011' };
    model.create.mockResolvedValue({ toObject: () => ({ ...dto, coach: user.sub }) });

    const created = await service.create(dto, user);
    expect(model.create).toHaveBeenCalled();
    expect((created as any).coach).toBe(user.sub);
  });

  it('should list activities with pagination', async () => {
    model.exec
      .mockResolvedValueOnce([{ nom_activite: 'A' }])
      .mockResolvedValueOnce(1);

    const res = await service.findAll({ page: 1, limit: 10 } as any);
    expect(res.total).toBe(1);
    expect(res.items.length).toBe(1);
  });

  it('should update any activity as academie (even created by coach)', async () => {
    const id = '507f1f77bcf86cd799439012';
    const user = { role: UserRole.ACADEMIE, sub: '507f1f77bcf86cd799439012' };
    // Académie peut modifier une activité créée par un coach
    const activity = { _id: id, coach: '507f1f77bcf86cd799439099' };
    model.exec
      .mockResolvedValueOnce(activity) // findById
      .mockResolvedValueOnce({ ...activity, nom_activite: 'New' }); // findByIdAndUpdate

    const updated = await service.update(id, { nom_activite: 'New' }, user as any);
    expect(updated).toBeDefined();
  });

  it('should remove own activity as coach', async () => {
    const id = '507f1f77bcf86cd799439011';
    const user = { role: UserRole.COACH, sub: id };
    const activity = { _id: id, coach: id };
    model.exec
      .mockResolvedValueOnce(activity) // findById
      .mockResolvedValueOnce({}); // findByIdAndDelete

    const res = await service.remove(id, user as any);
    expect(res.deleted).toBe(true);
  });

  it('should remove any activity as academie', async () => {
    const id = '507f1f77bcf86cd799439013';
    const user = { role: UserRole.ACADEMIE, sub: '507f1f77bcf86cd799439012' };
    // Académie peut supprimer une activité créée par un coach
    const activity = { _id: id, coach: '507f1f77bcf86cd799439099' };
    model.exec
      .mockResolvedValueOnce(activity) // findById
      .mockResolvedValueOnce({}); // findByIdAndDelete

    const res = await service.remove(id, user as any);
    expect(res.deleted).toBe(true);
  });
});


