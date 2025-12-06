
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
import { Enrollment, EnrollmentDocument } from '../enrollments/entity/enrollment.entity';

import { UsersService } from '../users/users.service';
import { FirebaseService } from '../common/firebase.service';

@Injectable()
export class ProgramsService {
  constructor(
    @InjectModel(Program.name) private readonly programModel: Model<ProgramDocument>,
    @InjectModel(Activity.name) private readonly activityModel: Model<ActivityDocument>,
    @InjectModel(Enrollment.name) private readonly enrollmentModel: Model<EnrollmentDocument>,
    private readonly usersService: UsersService,
    private readonly firebaseService: FirebaseService,
  ) { }

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

    // Add enrollment count for each program
    const itemsWithEnrollments = await Promise.all(
      items.map(async (program) => {
        const nombreInscrits = await this.enrollmentModel
          .countDocuments({ program: program._id })
          .exec();

        const programObj = program.toObject();
        return {
          ...programObj,
          nombreInscrits,
        };
      })
    );

    return {
      total,
      page: Number(page),
      limit: Number(limit),
      items: itemsWithEnrollments,
    };
  }

  async findOne(id: string) {
    const program = await this.programModel.findById(this.ensureObjectId(id)).populate('activites').exec();
    if (!program) {
      throw new NotFoundException('Programme introuvable');
    }

    // Count enrollments for this program
    const nombreInscrits = await this.enrollmentModel
      .countDocuments({ program: program._id })
      .exec();

    // Convert to plain object and add enrollment count
    const programObj = program.toObject();
    return {
      ...programObj,
      nombreInscrits,
    };
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

    // Notify parents of enrolled children about the update
    this.notifyParentsOfUpdate(program._id, program.nom_programme);

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

    // Notify parents of enrolled children about the update
    this.notifyParentsOfUpdate(program._id, program.nom_programme);

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

  private async notifyParentsOfUpdate(programId: Types.ObjectId, programName: string) {
    try {
      console.log(`[NOTIFICATION] Starting notification process for program: ${programName} (${programId})`);

      // Find all enrollments for this program
      const enrollments = await this.enrollmentModel.find({ program: programId }).exec();
      console.log(`[NOTIFICATION] Found ${enrollments.length} enrollments`);

      if (!enrollments.length) {
        console.log('[NOTIFICATION] No enrollments found, skipping notifications');
        return;
      }

      // Get unique child IDs
      const childIds = [...new Set(enrollments.map(e => e.child.toString()))];
      console.log(`[NOTIFICATION] Unique child IDs: ${childIds.length}`, childIds);

      // Find parents of these children
      const children = await Promise.all(childIds.map(id => this.usersService.findById(id)));
      console.log(`[NOTIFICATION] Found ${children.length} children`);

      const parentIds = new Set<string>();
      children.forEach(child => {
        if (child && child.parent) {
          // Handle both ObjectId and populated User object
          const parentId = typeof child.parent === 'object' && child.parent._id
            ? child.parent._id.toString()
            : child.parent.toString();
          parentIds.add(parentId);
          console.log(`[NOTIFICATION] Child ${child._id} has parent ${parentId}`);
        } else {
          console.log(`[NOTIFICATION] Child ${child?._id} has no parent`);
        }
      });

      console.log(`[NOTIFICATION] Found ${parentIds.size} unique parents`);

      if (parentIds.size === 0) {
        console.log('[NOTIFICATION] No parents found, skipping notifications');
        return;
      }

      // Fetch parents to get their FCM tokens
      const parents = await Promise.all(Array.from(parentIds).map(id => this.usersService.findById(id)));
      console.log(`[NOTIFICATION] Fetched ${parents.length} parent users`);

      const tokens: string[] = [];
      parents.forEach(parent => {
        if (parent && parent.fcmToken) {
          tokens.push(parent.fcmToken);
          console.log(`[NOTIFICATION] Parent ${parent._id} has FCM token: ${parent.fcmToken.substring(0, 20)}...`);
        } else {
          console.log(`[NOTIFICATION] Parent ${parent?._id} has NO FCM token`);
        }
      });

      console.log(`[NOTIFICATION] Total FCM tokens collected: ${tokens.length}`);

      if (tokens.length > 0) {
        console.log(`[NOTIFICATION] Sending notifications to ${tokens.length} parents...`);
        await this.firebaseService.sendMulticastNotification(
          tokens,
          'Mise à jour du programme',
          `Le programme "${programName}" a été mis à jour. Vérifiez les nouveaux détails !`,
          { programId: programId.toString() }
        );
        console.log('[NOTIFICATION] Notifications sent successfully!');
      } else {
        console.log('[NOTIFICATION] No FCM tokens available, skipping notification send');
      }

    } catch (error) {
      console.error('[NOTIFICATION] Error notifying parents:', error);
    }
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

