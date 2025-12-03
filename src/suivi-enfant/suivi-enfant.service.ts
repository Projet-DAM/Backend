
  import * as mongoose from 'mongoose';

  import { Injectable, NotFoundException, BadRequestException, ForbiddenException, Logger } from '@nestjs/common';
  import { InjectModel } from '@nestjs/mongoose';
  import { Model, Types } from 'mongoose';
  import { SuiviEnfant, SuiviEnfantDocument } from './suivi-enfant.schema';
  import { CreateSuiviEnfantDto } from './dto/create-suivi-enfant.dto';
  import { UpdateSuiviEnfantDto } from './dto/update-suivi-enfant.dto';
  import { UsersService } from '../users/users.service';
  import { UserRole } from '../users/interfaces/user-role.enum';

@Injectable()
export class SuiviEnfantService {
  private readonly logger = new Logger(SuiviEnfantService.name);
  constructor(
    @InjectModel(SuiviEnfant.name)
    private readonly suiviModel: Model<SuiviEnfantDocument>,
    private readonly usersService: UsersService
  ) {}

    async getAvailableParents(userId: string): Promise<any[]> {
    try {
      if (!userId || !mongoose.Types.ObjectId.isValid(userId)) {
        throw new BadRequestException('id invalide');
      }
      this.logger.log(`🔍 Getting parents for coach: ${userId}`);
      const suivis = await this.suiviModel.find({
        $or: [
          { coach: userId },
          { coach: new Types.ObjectId(userId) }
        ]
      }).populate({
        path: 'enfant',
        populate: {
          path: 'parent',
          select: '_id nom prenom email role photoProfil'
        }
      }).exec();
      this.logger.log(`📊 Found ${suivis.length} suivis for coach ${userId}`);
      if (suivis.length === 0) {
        this.logger.warn('⚠️ No suivis found for this coach');
        return [];
      }
      const parentMap = new Map();
      for (const suivi of suivis) {
        // enfant peut être un ObjectId ou un document peuplé
        const enfant: any = suivi.enfant;
        let parent = null;
        if (enfant && typeof enfant === 'object' && 'parent' in enfant) {
          parent = enfant.parent;
        }
        if (parent && typeof parent === 'object' && '_id' in parent) {
          const parentAny = parent as any;
          const parentId = parentAny._id.toString();
          if (!parentMap.has(parentId)) {
            parentMap.set(parentId, {
              _id: parentAny._id,
              nom: parentAny.nom,
              prenom: parentAny.prenom,
              email: parentAny.email,
              role: parentAny.role,
              photoProfil: parentAny.photoProfil
            });
          }
        }
      }
      const parents = Array.from(parentMap.values());
      this.logger.log(`✅ Returning ${parents.length} unique parents`);
      return parents;
    } catch (error) {
      this.logger.error('❌ Error in getAvailableParents:', error);
      throw error;
    }
    }

    async getAvailableCoaches(userId: string): Promise<any[]> {
      if (!userId || !mongoose.Types.ObjectId.isValid(userId)) {
        throw new BadRequestException('id invalide');
      }
      const children = await this.usersService.getChildren(userId);
      const childIds = children.map(child => child._id);
      const suivis = await this.suiviModel.find({ enfant: { $in: childIds } }).populate('coach');
      const coachMap = new Map();
      suivis.forEach(suivi => {
        if (suivi.coach && !coachMap.has(suivi.coach._id.toString())) {
          coachMap.set(suivi.coach._id.toString(), suivi.coach);
        }
      });
      return Array.from(coachMap.values());
    }


  /**
   * Retourne la liste des parents ayant au moins un enfant suivi par ce coach
   */
  async getParentsForCoach(coachId: string): Promise<Array<{ _id: string; nom: string; prenom: string; email: string }>> {
    // Trouver tous les suivis faits par ce coach
    const suivis = await this.suiviModel.find({ coach: coachId }).populate({ path: 'enfant', populate: { path: 'parent' } }).exec();
    const parentMap = new Map<string, { _id: string; nom: string; prenom: string; email: string }>();
    for (const suivi of suivis) {
      const enfant: any = suivi.enfant;
      if (enfant && enfant.parent && enfant.parent._id) {
        const parent = enfant.parent;
        if (!parentMap.has(parent._id.toString())) {
          parentMap.set(parent._id.toString(), {
            _id: parent._id.toString(),
            nom: parent.nom,
            prenom: parent.prenom,
            email: parent.email,
          });
        }
      }
    }
    return Array.from(parentMap.values());
  }

  /**
   * Retourne la liste des coachs ayant fait un suivi pour au moins un enfant de ce parent
   */
  async getCoachesForParent(parentId: string): Promise<Array<{ _id: string; nom: string; prenom: string; email: string }>> {
    // Récupérer les enfants du parent
    const enfants = await this.usersService.getChildren(parentId);
    const enfantIds = enfants.map(e => e._id);
    // Trouver tous les suivis pour ces enfants
    const suivis = await this.suiviModel.find({ enfant: { $in: enfantIds } }).populate('coach').exec();
    const coachMap = new Map<string, { _id: string; nom: string; prenom: string; email: string }>();
    for (const suivi of suivis) {
      const coach: any = suivi.coach;
      if (coach && coach._id) {
        if (!coachMap.has(coach._id.toString())) {
          coachMap.set(coach._id.toString(), {
            _id: coach._id.toString(),
            nom: coach.nom,
            prenom: coach.prenom,
            email: coach.email,
          });
        }
      }
    }
    return Array.from(coachMap.values());
  }

  async create(dto: CreateSuiviEnfantDto, currentUser: { userId: string; role: UserRole }): Promise<SuiviEnfant> {
    if (!currentUser || currentUser.role !== UserRole.COACH) {
      throw new ForbiddenException('Seul un coach peut créer un suivi');
    }
    if (!Types.ObjectId.isValid(dto.enfantId)) {
      throw new BadRequestException('enfantId invalide');
    }

    const enfant = await this.usersService.findById(dto.enfantId);
    if (!enfant) {
      throw new NotFoundException('Enfant non trouvé');
    }
    if (enfant.role !== UserRole.ENFANT) {
      throw new BadRequestException('enfantId doit référencer un utilisateur avec le rôle ENFANT');
    }

    // Previously we required the coach to be associated to the child and auto-attached
    // the coach to the child's `coach` array. That constraint has been removed so
    // any coach can create a suivi for any child. We still resolve the child and
    // validate its role.

    // Derive enfantName from provided dto or the enfant user record
    const enfantName = dto.enfantName ?? `${enfant.prenom ?? ''} ${enfant.nom ?? ''}`.trim();

    const suivi = await this.suiviModel.create({
      ...dto,
      enfant: new Types.ObjectId(dto.enfantId),
      enfantName,
      coach: new Types.ObjectId(currentUser.userId),
    });
    // Return the created suivi populated to help the frontend update UI without refetch
    const populated = await this.suiviModel.findById(suivi._id).populate('enfant').exec();
    if (!populated) throw new NotFoundException('Erreur lors de la création du suivi');
    return populated;
  }

  // Helper to extract an object id string from various stored shapes
  private extractId(raw: any): string | undefined {
    if (!raw) return undefined;
    // If it's already a string and looks like an ObjectId hex
    if (typeof raw === 'string') return raw;
    // If it's a Mongoose ObjectId
    try {
      if (Types.ObjectId.isValid(raw)) return raw.toString();
    } catch (e) {
      // ignore
    }
    // If populated document with _id
    if (raw._id) {
      try {
        if (typeof raw._id.toString === 'function') return raw._id.toString();
        return String(raw._id);
      } catch (e) {
        return String(raw._id);
      }
    }
    // Fallback: try toString and detect 24-hex
    if (typeof raw.toString === 'function') {
      const s = raw.toString();
      if (/^[0-9a-fA-F]{24}$/.test(s)) return s;
    }
    return undefined;
  }

  // Helper to check whether a parent (by id) owns an enfant (by id)
  private async parentOwnsChild(parentId: string, enfantId: string): Promise<boolean> {
    if (!parentId || !enfantId) return false;
    // Try to load the parent user and check their enfants array
    try {
      const parent = await this.usersService.findById(parentId);
      if (!parent) return false;
      // normalize enfants array
      const enfants = (parent as any).enfants || [];
      for (const e of enfants) {
        const id = this.extractId(e) || (typeof e === 'string' ? e : undefined);
        if (id === enfantId) return true;
      }
    } catch (e) {
      // ignore
    }
    return false;
  }

  async findAll(currentUser: { userId: string; role: UserRole }): Promise<SuiviEnfant[]> {
    if (!currentUser) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    if (currentUser.role === UserRole.ACADEMIE) {
      return this.suiviModel.find().populate('enfant').exec();
    }

    if (currentUser.role === UserRole.COACH) {
      // Coaches see all suivis on their home screen.
      return this.suiviModel.find().populate('enfant').exec();
    }

    if (currentUser.role === UserRole.PARENT) {
      const children = await this.usersService.getChildren(currentUser.userId);
      if (!children || children.length === 0) {
        return [];
      }
      const childIds = children.map((c) => c._id);
      // Support both stored shapes for `enfant`: direct ObjectId or embedded object with _id
      const normalizedIds = childIds.map((id) => (Types.ObjectId.isValid(id) ? new Types.ObjectId(id) : id));
      return this.suiviModel
        .find({ $or: [{ enfant: { $in: normalizedIds } }, { 'enfant._id': { $in: normalizedIds } }] })
        .populate('enfant')
        .exec();
    }

    throw new ForbiddenException('Rôle non autorisé pour cette opération');
  }

  async findOne(id: string, currentUser: { userId: string; role: UserRole }): Promise<SuiviEnfant> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const suivi = await this.suiviModel.findById(id).populate('enfant').exec();
    if (!suivi) throw new NotFoundException('Suivi non trouvé');
    if (!currentUser) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    if (currentUser.role === UserRole.ACADEMIE) {
      return suivi;
    }

    // Charger l'enfant pour vérifier le parent / coach
    // Support cases where `enfant` is either an ObjectId ref or an embedded object { _id, prenom, nom }
    const rawEnfant = (suivi as any).enfant;
    const enfantId = rawEnfant && rawEnfant._id ? rawEnfant._id.toString() : rawEnfant.toString();
    const enfant = await this.usersService.findById(enfantId);
    if (!enfant) {
      throw new NotFoundException('Enfant non trouvé pour ce suivi');
    }
    const childParentId = this.extractId((enfant as any).parent);
    const childCreatedBy = this.extractId((enfant as any).createdBy);
    this.logger.debug(`findOne: suiviId=${id} enfantId=${enfantId} enfant.parent=${childParentId} enfant.createdBy=${childCreatedBy} requester=${currentUser.userId}`);

    if (currentUser.role === UserRole.PARENT) {
      // primary checks
      let allowed = (childParentId === currentUser.userId) || (childCreatedBy === currentUser.userId);
      if (!allowed) {
        // fallback: check parent's enfants membership
        allowed = await this.parentOwnsChild(currentUser.userId, enfantId);
        if (allowed) this.logger.debug(`findOne: allowed by parent.enfants membership for requester=${currentUser.userId}`);
      }
      // additional robust fallback: fetch the parent's children list and check IDs
      if (!allowed) {
        try {
          const parentsChildren = await this.usersService.getChildren(currentUser.userId);
          const ids = (parentsChildren || []).map((c: any) => this.extractId(c._id) || (c._id ? c._id.toString() : undefined)).filter(Boolean);
          if (ids.includes(enfantId)) {
            allowed = true;
            this.logger.debug(`findOne: allowed by usersService.getChildren for requester=${currentUser.userId}`);
          }
        } catch (e) {
          // ignore and proceed to deny if not allowed
        }
      }
      if (!allowed) {
        this.logger.warn(`Unauthorized findOne: requester=${currentUser.userId} cannot access suivi=${id} for enfant=${enfantId}`);
        throw new ForbiddenException('Ce suivi n\'appartient pas à un enfant de ce parent');
      }
      return suivi;
    }

    if (currentUser.role === UserRole.COACH) {
      // Allow coaches but keep the option to restrict to creator. Currently we allow any coach to view.
      // If stored coach is an ObjectId or embedded, normalize before comparing.
      const suiviCoach = (suivi as any).coach;
      const suiviCoachId = suiviCoach && suiviCoach._id ? suiviCoach._id.toString() : (suiviCoach ? suiviCoach.toString() : undefined);
      if (suiviCoachId && suiviCoachId !== currentUser.userId) {
        // do not block; comment retained to show previous stricter behavior
      }
      return suivi;
    }

    throw new ForbiddenException('Rôle non autorisé pour cette opération');
  }

  async findByEnfant(enfantId: string, currentUser: { userId: string; role: UserRole }): Promise<SuiviEnfant[]> {
    if (!Types.ObjectId.isValid(enfantId)) throw new BadRequestException('enfantId invalide');
    const enfant = await this.usersService.findById(enfantId);
    if (!enfant) {
      throw new NotFoundException('Enfant non trouvé');
    }
    if (!currentUser) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    if (currentUser.role === UserRole.ACADEMIE) {
      return this.suiviModel
        .find({ $or: [{ enfant: new Types.ObjectId(enfantId) }, { 'enfant._id': new Types.ObjectId(enfantId) }] })
        .populate('enfant')
        .exec();
    }

    if (currentUser.role === UserRole.PARENT) {
      const childParentId = this.extractId((enfant as any).parent);
      const childCreatedBy = this.extractId((enfant as any).createdBy);
      this.logger.debug(`findByEnfant: enfantId=${enfantId} parent=${childParentId} createdBy=${childCreatedBy} requester=${currentUser.userId}`);
      // Primary check: parent or createdBy matches requester
      let allowed = (childParentId === currentUser.userId) || (childCreatedBy === currentUser.userId);
      // Fallback: check parent's enfants array (some records may only list children on the parent doc)
      if (!allowed) {
        allowed = await this.parentOwnsChild(currentUser.userId, enfantId);
        if (allowed) this.logger.debug(`findByEnfant: allowed by parent.enfants membership for requester=${currentUser.userId}`);
      }
      // Additional fallback: check parent's children via usersService.getChildren
      if (!allowed) {
        try {
          const parentsChildren = await this.usersService.getChildren(currentUser.userId);
          const ids = (parentsChildren || []).map((c: any) => this.extractId(c._id) || (c._id ? c._id.toString() : undefined)).filter(Boolean);
          if (ids.includes(enfantId)) {
            allowed = true;
            this.logger.debug(`findByEnfant: allowed by usersService.getChildren for requester=${currentUser.userId}`);
          }
        } catch (e) {
          // ignore
        }
      }
      if (!allowed) {
        this.logger.warn(`Unauthorized access attempt: requester=${currentUser.userId} tried to access enfant=${enfantId}`);
        throw new ForbiddenException('Cet enfant n\'appartient pas à ce parent');
      }
      return this.suiviModel
        .find({ $or: [{ enfant: new Types.ObjectId(enfantId) }, { 'enfant._id': new Types.ObjectId(enfantId) }] })
        .populate('enfant')
        .exec();
    }

    if (currentUser.role === UserRole.COACH) {
      // Coaches are allowed to view suivis for any child; do not require prior association.
      return this.suiviModel
        .find({ $or: [{ enfant: new Types.ObjectId(enfantId) }, { 'enfant._id': new Types.ObjectId(enfantId) }] })
        .populate('enfant')
        .exec();
    }

    throw new ForbiddenException('Rôle non autorisé pour cette opération');
  }

  async update(id: string, dto: UpdateSuiviEnfantDto, currentUser: { userId: string; role: UserRole }): Promise<SuiviEnfant> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const existing = await this.suiviModel.findById(id).exec();
    if (!existing) throw new NotFoundException('Suivi non trouvé');

    if (!currentUser) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    // Parents may update suivis only for their own children
    if (currentUser.role === UserRole.PARENT) {
      // Normalize existing.enfant which can be an ObjectId or embedded object
      const rawEnfant = (existing as any).enfant;
      const enfantId = rawEnfant && rawEnfant._id ? rawEnfant._id.toString() : rawEnfant.toString();
      const enfant = await this.usersService.findById(enfantId);
      const childParentId = this.extractId(enfant && (enfant as any).parent);
      const childCreatedBy = this.extractId(enfant && (enfant as any).createdBy);
      if (!enfant || (childParentId !== currentUser.userId && childCreatedBy !== currentUser.userId)) {
        throw new ForbiddenException("Cet enfant n'appartient pas à ce parent");
      }
    } else if (currentUser.role === UserRole.COACH) {
      // Coaches are allowed to update suivis (no further restriction here).
      // If you prefer to restrict updates to the suivi creator, uncomment the check below.
      // if (!existing.coach || existing.coach.toString() !== currentUser.userId) {
      //   throw new ForbiddenException('Ce suivi n\'a pas été créé par ce coach');
      // }
    } else {
      throw new ForbiddenException('Rôle non autorisé pour cette opération');
    }

    const suivi = await this.suiviModel.findByIdAndUpdate(id, dto, { new: true }).exec();
    if (!suivi) throw new NotFoundException('Suivi non trouvé');
    return suivi;
  }

  async remove(id: string, currentUser: { userId: string; role: UserRole }): Promise<void> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const existing = await this.suiviModel.findById(id).exec();
    if (!existing) throw new NotFoundException('Suivi non trouvé');

    if (!currentUser || currentUser.role !== UserRole.COACH) {
      throw new ForbiddenException('Seul un coach peut supprimer un suivi');
    }

    if (!existing.coach || existing.coach.toString() !== currentUser.userId) {
      throw new ForbiddenException('Ce suivi n\'a pas été créé par ce coach');
    }

    const res = await this.suiviModel.findByIdAndDelete(id).exec();
    if (!res) throw new NotFoundException('Suivi non trouvé');
  }
}
