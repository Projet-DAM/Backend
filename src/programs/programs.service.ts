import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model, Types } from 'mongoose';
import { Program, ProgramDocument } from './schemas/program.schema';
import { CreateProgramDto } from './dto/create-program.dto';
import { UpdateProgramDto } from './dto/update-program.dto';
import { QueryProgramDto } from './dto/query-program.dto';
import { ManageProgramActivitiesDto } from './dto/manage-program-activities.dto';
import { Activity, ActivityDocument } from '../activities/schemas/activity.schema';
import { UserRole } from '../users/interfaces/user-role.enum';

@Injectable()
export class ProgramsService {
  constructor(
    @InjectModel(Program.name) private readonly programModel: Model<ProgramDocument>,
    @InjectModel(Activity.name) private readonly activityModel: Model<ActivityDocument>,
  ) {}

  async create(dto: CreateProgramDto, user: any) {
    this.ensureCreatorRole(user);

    const { activites, ...rest } = dto;
    const activityIds = await this.validateActivities(activites, user);
    const payload: Partial<Program> = {
      ...rest,
      activites: activityIds,
    };

    const userObjectId = this.extractUserObjectId(user);
    if (user.role === UserRole.COACH) {
      payload.coach = userObjectId;
    }
    if (user.role === UserRole.ACADEMIE) {
      payload.academie = userObjectId;
    }

    const program = await this.programModel.create(payload);
    await this.syncActivityLinks(program._id, activityIds);
    return program;
  }

  async findAll(query: QueryProgramDto) {
    const {
      nom,
      coach,
      academie,
      statut,
      page = 1,
      limit = 10,
      sortBy = 'createdAt',
      order = 'desc',
    } = query;

    const filter: FilterQuery<ProgramDocument> = {};
    if (nom) {
      filter.nom_programme = { $regex: nom, $options: 'i' };
    }
    if (coach) {
      filter.coach = new Types.ObjectId(coach);
    }
    if (academie) {
      filter.academie = new Types.ObjectId(academie);
    }
    if (statut) {
      filter.statut = statut;
    }

    const sort: Record<string, 1 | -1> = { [sortBy]: order === 'desc' ? -1 : 1 };
    const skip = (Number(page) - 1) * Number(limit);

    const [items, total] = await Promise.all([
      this.programModel
        .find(filter)
        .populate('activites')
        .sort(sort)
        .skip(skip)
        .limit(Number(limit))
        .exec(),
      this.programModel.countDocuments(filter).exec(),
    ]);

    return {
      total,
      page: Number(page),
      limit: Number(limit),
      items,
    };
  }

  async findOne(id: string) {
    const program = await this.programModel.findById(this.ensureObjectId(id)).populate('activites').exec();
    if (!program) {
      throw new NotFoundException('Programme introuvable');
    }
    return program;
  }

  async update(id: string, dto: UpdateProgramDto, user: any) {
    this.ensureCreatorRole(user);
    const program = await this.programModel.findById(this.ensureObjectId(id)).exec();
    if (!program) {
      throw new NotFoundException('Programme introuvable');
    }
    this.assertOwnership(program, user);

    const { activites, ...rest } = dto;
    const updatePayload: Partial<Program> = { ...rest };

    if (activites) {
      const activityIds = await this.validateActivities(activites, user, program._id);
      updatePayload.activites = activityIds;
      await this.syncActivityLinks(program._id, activityIds);
    }

    const updated = await this.programModel
      .findByIdAndUpdate(program._id, updatePayload, { new: true })
      .populate('activites')
      .exec();

    return updated;
  }

  async updateActivities(id: string, dto: ManageProgramActivitiesDto, user: any) {
    this.ensureCreatorRole(user);
    const program = await this.programModel.findById(this.ensureObjectId(id)).exec();
    if (!program) {
      throw new NotFoundException('Programme introuvable');
    }
    this.assertOwnership(program, user);

    const activityIds = await this.validateActivities(dto.activites, user, program._id);
    await this.programModel.findByIdAndUpdate(
      program._id,
      { activites: activityIds },
      { new: true },
    );
    await this.syncActivityLinks(program._id, activityIds);

    return this.findOne(id);
  }

  async remove(id: string, user: any) {
    this.ensureCreatorRole(user);
    const program = await this.programModel.findById(this.ensureObjectId(id)).exec();
    if (!program) {
      throw new NotFoundException('Programme introuvable');
    }
    this.assertOwnership(program, user);

    await this.syncActivityLinks(program._id, []);
    await this.programModel.findByIdAndDelete(program._id).exec();

    return { deleted: true };
  }

  private ensureCreatorRole(user: any) {
    if (![UserRole.ACADEMIE, UserRole.COACH].includes(user?.role)) {
      throw new ForbiddenException('Seuls les coachs et académies peuvent gérer les programmes');
    }
  }

  private ensureObjectId(id: string) {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('Identifiant invalide');
    }
    return new Types.ObjectId(id);
  }

  private extractUserObjectId(user: any) {
    const id = user?.userId || user?.sub || user?._id || user?.id;
    if (!id || !Types.ObjectId.isValid(id)) {
      throw new BadRequestException("Impossible de déterminer l'utilisateur courant");
    }
    return new Types.ObjectId(id);
  }

  private async validateActivities(
    activityIds: string[] | undefined,
    user: any,
    programId?: Types.ObjectId,
  ): Promise<Types.ObjectId[]> {
    if (!activityIds || activityIds.length === 0) {
      return [];
    }

    const uniqueIds = Array.from(new Set(activityIds));
    uniqueIds.forEach((id) => {
      if (!Types.ObjectId.isValid(id)) {
        throw new BadRequestException(`Identifiant d'activité invalide: ${id}`);
      }
    });

    const objectIds = uniqueIds.map((id) => new Types.ObjectId(id));
    const activities = await this.activityModel
      .find({ _id: { $in: objectIds } })
      .select(['coach', 'academie', 'programme'])
      .exec();

    if (activities.length !== objectIds.length) {
      throw new NotFoundException('Une ou plusieurs activités sont introuvables');
    }

    const userId = this.extractUserObjectId(user);

    for (const activity of activities) {
      if (user.role === UserRole.COACH && (!activity.coach || !userId.equals(activity.coach as Types.ObjectId))) {
        throw new ForbiddenException('Vous ne pouvez utiliser que vos propres activités');
      }
      if (user.role === UserRole.ACADEMIE && activity.academie && !userId.equals(activity.academie as Types.ObjectId)) {
        throw new ForbiddenException("Vous ne pouvez utiliser que les activités de votre académie");
      }
      if (
        activity.programme &&
        (!programId || !(activity.programme as Types.ObjectId).equals(programId))
      ) {
        throw new BadRequestException('Une activité est déjà assignée à un autre programme');
      }
    }

    return objectIds;
  }

  private async syncActivityLinks(programId: Types.ObjectId, activityIds: Types.ObjectId[]) {
    await this.activityModel.updateMany(
      { programme: programId },
      { $unset: { programme: '' } },
    );

    if (activityIds.length > 0) {
      await this.activityModel.updateMany(
        { _id: { $in: activityIds } },
        { $set: { programme: programId } },
      );
    }
  }

  private assertOwnership(program: ProgramDocument, user: any) {
    if (user.role === UserRole.ACADEMIE) {
      const userId = this.extractUserObjectId(user);
      if (program.academie && !userId.equals(program.academie as Types.ObjectId)) {
        throw new ForbiddenException('Programme non rattaché à votre académie');
      }
      return;
    }

    if (user.role === UserRole.COACH) {
      const userId = this.extractUserObjectId(user);
      if (!program.coach || !userId.equals(program.coach as Types.ObjectId)) {
        throw new ForbiddenException('Programme non rattaché à ce coach');
      }
    }
  }
}

