import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model, Types } from 'mongoose';
import { Activity, ActivityDocument, ActivityStatus } from './schemas/activity.schema';
import { CreateActivityDto } from './dto/create-activity.dto';
import { UpdateActivityDto } from './dto/update-activity.dto';
import { QueryActivityDto } from './dto/query-activity.dto';
import { UserRole } from '../users/interfaces/user-role.enum';
import { Program, ProgramDocument } from '../programs/schemas/program.schema';

@Injectable()
export class ActivitiesService {
  constructor(
    @InjectModel(Activity.name) private readonly activityModel: Model<ActivityDocument>,
    @InjectModel(Program.name) private readonly programModel: Model<ProgramDocument>,
  ) {}

  async create(dto: CreateActivityDto, user: any): Promise<Activity> {
    if (![UserRole.ACADEMIE, UserRole.COACH].includes(user.role)) {
      throw new ForbiddenException('Seuls les coachs et académies peuvent créer une activité');
    }

    const payload: Partial<Activity> = { ...dto } as any;

    if (dto.date) {
      (payload as any).date = new Date(dto.date);
      if (isNaN((payload as any).date.getTime())) {
        throw new BadRequestException('Date invalide');
      }
    }

    if (user.role === UserRole.COACH) {
      (payload as any).coach = new Types.ObjectId(user.userId || user.sub || user._id || user.id);
    }
    if (user.role === UserRole.ACADEMIE) {
      (payload as any).academie = new Types.ObjectId(user.userId || user.sub || user._id || user.id);
    }

    const created = await this.activityModel.create(payload);
    return created.toObject() as any;
  }

  async findAll(query: QueryActivityDto) {
    const {
      categorie,
      date,
      coach,
      academie,
      programme,
      statut,
      page = 1,
      limit = 10,
      sortBy = 'date',
      order = 'asc',
    } = query;

    const filter: FilterQuery<ActivityDocument> = {};
    if (categorie) filter.categorie = categorie;
    if (date) {
      const d = new Date(date);
      if (isNaN(d.getTime())) {
        throw new BadRequestException('Paramètre date invalide, attendu YYYY-MM-DD');
      }
      const next = new Date(d);
      next.setDate(d.getDate() + 1);
      filter.date = { $gte: d, $lt: next } as any;
    }
    if (coach) filter.coach = new Types.ObjectId(coach);
    if (academie) filter.academie = new Types.ObjectId(academie);
    if (programme) filter.programme = new Types.ObjectId(programme);
    if (statut) filter.statut = statut as ActivityStatus;

    const sort: Record<string, 1 | -1> = { [sortBy]: order === 'desc' ? -1 : 1 };
    const skip = (Number(page) - 1) * Number(limit);

    const [items, total] = await Promise.all([
      this.activityModel
        .find(filter)
        .sort(sort)
        .skip(skip)
        .limit(Number(limit))
        .exec(),
      this.activityModel.countDocuments(filter).exec(),
    ]);

    return {
      total,
      page: Number(page),
      limit: Number(limit),
      items,
    };
  }

  async findOne(id: string): Promise<Activity> {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }
    const activity = await this.activityModel.findById(id).exec();
    if (!activity) throw new NotFoundException('Activité non trouvée');
    return activity;
  }

  async update(id: string, dto: UpdateActivityDto, user: any): Promise<Activity> {
    if (![UserRole.ACADEMIE, UserRole.COACH].includes(user.role)) {
      throw new ForbiddenException('Seuls les coachs et académies peuvent modifier une activité');
    }
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }
    const activity = await this.activityModel.findById(id).exec();
    if (!activity) throw new NotFoundException('Activité non trouvée');

    this.assertOwnership(activity, user);

    const update: any = { ...dto };
    if (dto.date) {
      const d = new Date(dto.date as any);
      if (isNaN(d.getTime())) throw new BadRequestException('Date invalide');
      update.date = d;
    }

    const updated = await this.activityModel
      .findByIdAndUpdate(id, update, { new: true })
      .exec();
    return updated as any;
  }

  async remove(id: string, user: any): Promise<{ deleted: boolean }> {
    if (![UserRole.ACADEMIE, UserRole.COACH].includes(user.role)) {
      throw new ForbiddenException('Seuls les coachs et académies peuvent supprimer une activité');
    }
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }
    const activity = await this.activityModel.findById(id).exec();
    if (!activity) throw new NotFoundException('Activité non trouvée');

    this.assertOwnership(activity, user);

    await this.activityModel.findByIdAndDelete(id).exec();
    await this.programModel
      .updateMany(
        { activites: activity._id },
        { $pull: { activites: activity._id } },
      )
      .exec();
    return { deleted: true };
  }

  private assertOwnership(activity: ActivityDocument, user: any) {
    // Les académies ont accès à toutes les activités
    if (user.role === UserRole.ACADEMIE) {
      return; // Autoriser sans vérification
    }
    
    // Les coachs n'ont accès qu'à leurs propres activités
    if (user.role === UserRole.COACH) {
      if (!activity.coach) {
        throw new ForbiddenException('Action non autorisée sur cette activité');
      }
      
      const userId = user.userId || user.sub || user._id || user.id;
      const userObjectId = new Types.ObjectId(userId);
      const activityCoachId = new Types.ObjectId(activity.coach);
      
      if (!userObjectId.equals(activityCoachId)) {
        throw new ForbiddenException('Action non autorisée sur cette activité');
      }
    }
  }
}


