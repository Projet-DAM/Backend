import { Injectable, NotFoundException, ConflictException, BadRequestException, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import * as bcrypt from 'bcrypt';
import { User, UserDocument } from './entity/user.entity';
import { UserRole } from './interfaces/user-role.enum';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { CreateChildDto } from './dto/create-child.dto';

@Injectable()
export class UsersService {
  private readonly logger = new Logger(UsersService.name);

  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
  ) { }

  async findById(id: string): Promise<UserDocument | null> {
    if (!Types.ObjectId.isValid(id)) return null;
    return this.userModel.findById(id)
      .populate('enfants')
      .populate('parent')
      .populate('coach')
      .exec();
  }

  async findByEmail(email: string): Promise<UserDocument | null> {
    return this.userModel.findOne({ email }).exec();
  }

  async findOne(id: string): Promise<UserDocument | null> {
    return this.findById(id);
  }

  async create(createUserDto: CreateUserDto): Promise<UserDocument> {
    if (createUserDto.email && !createUserDto.email.includes('@temp.com')) {
      const existingUser = await this.userModel.findOne({ email: createUserDto.email });
      if (existingUser) {
        throw new ConflictException('Cet email est déjà utilisé');
      }
    }

    const passwordToHash = createUserDto.motDePasse || 'temp123';
    const hashedPassword = await bcrypt.hash(passwordToHash, 10);

    const userData: any = {
      ...createUserDto,
      motDePasse: hashedPassword,
    };

    if (createUserDto.dateNaissance) {
      userData.dateNaissance = new Date(createUserDto.dateNaissance);
    }

    if (createUserDto.role === UserRole.ENFANT) {
      delete userData.enfants;
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;

      if (createUserDto.parentId) {
        const parent = await this.userModel.findById(createUserDto.parentId);
        if (!parent) throw new NotFoundException('Parent non trouvé');
        if (parent.role !== UserRole.PARENT) throw new BadRequestException('L\'utilisateur spécifié comme parent doit avoir le rôle parent');
        userData.parent = new Types.ObjectId(createUserDto.parentId);
        delete userData.parentId;
      }
    } else if (createUserDto.role === UserRole.PARENT) {
      delete userData.parent;
      delete userData.coach;
      delete userData.dateNaissance;
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;
    } else if (createUserDto.role === UserRole.COACH) {
      delete userData.enfants;
      delete userData.parent;
      delete userData.coach;
      delete userData.dateNaissance;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;
    } else if (createUserDto.role === UserRole.ACADEMIE) {
      delete userData.enfants;
      delete userData.parent;
      delete userData.coach;
      delete userData.dateNaissance;
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
    }

    const user = new this.userModel(userData);
    const savedUser = await user.save();

    if (createUserDto.role === UserRole.ENFANT && savedUser.parent) {
      const parent = await this.userModel.findById(savedUser.parent);
      if (parent) {
        if (!parent.enfants) parent.enfants = [];
        if (!parent.enfants.some((id: any) => id.toString() === savedUser._id.toString())) {
          parent.enfants.push(savedUser._id);
          await parent.save();
        }
      }
    }

    return savedUser;
  }

  async findAll(filters?: { role?: UserRole; parentId?: string }): Promise<UserDocument[]> {
    const query: any = {};
    if (filters?.role) query.role = filters.role;
    if (filters?.parentId) {
      try {
        query.parent = new Types.ObjectId(filters.parentId);
      } catch (error) {
        throw new BadRequestException(`ID parent invalide: ${filters.parentId}`);
      }
    }

    return this.userModel.find(query)
      .populate('enfants')
      .populate('parent')
      .populate('coach')
      .exec();
  }

  async update(id: string, updateUserDto: UpdateUserDto): Promise<UserDocument> {
    const user = await this.userModel.findById(id);
    if (!user) throw new NotFoundException('Utilisateur non trouvé');

    if (updateUserDto.email && updateUserDto.email !== user.email) {
      const existingUser = await this.userModel.findOne({ email: updateUserDto.email });
      if (existingUser) throw new ConflictException('Cet email est déjà utilisé');
    }

    if (updateUserDto.dateNaissance) {
      (updateUserDto as any).dateNaissance = new Date(updateUserDto.dateNaissance);
    }

    if (updateUserDto.motDePasse) {
      updateUserDto.motDePasse = await bcrypt.hash(updateUserDto.motDePasse, 10);
    }

    Object.assign(user, updateUserDto);
    return user.save();
  }

  async remove(id: string): Promise<void> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const user = await this.userModel.findById(id);
    if (!user) throw new NotFoundException('Utilisateur non trouvé');

    if (user.role === UserRole.ENFANT && user.parent) {
      const parent = await this.userModel.findById(user.parent);
      if (parent && parent.enfants) {
        parent.enfants = parent.enfants.filter(childId => childId.toString() !== id);
        await parent.save();
      }
    }
    await this.userModel.findByIdAndDelete(id);
  }

  async removeChild(childId: string, parentId: string): Promise<{ success: boolean; childId: string }> {
    if (!Types.ObjectId.isValid(childId) || !Types.ObjectId.isValid(parentId)) {
      throw new BadRequestException('ID invalide');
    }
    const child = await this.userModel.findById(childId);
    if (!child) throw new NotFoundException("Enfant non trouvé");
    if (child.role !== UserRole.ENFANT) throw new BadRequestException("L'utilisateur n'est pas un enfant");

    let isAuthorized = false;
    if (child.parent && child.parent.toString() === parentId) isAuthorized = true;
    const requester = await this.userModel.findById(parentId);
    if (requester && requester.role === UserRole.ACADEMIE) isAuthorized = true;

    if (!isAuthorized) throw new BadRequestException("Non autorisé à supprimer cet enfant");

    await this.userModel.updateOne({ _id: parentId }, { $pull: { enfants: child._id } });
    await this.userModel.findByIdAndDelete(childId);
    return { success: true, childId };
  }

  async updateVerificationCode(userId: string, code: string, expiresAt: Date): Promise<void> {
    await this.userModel.updateOne({ _id: userId }, { verificationCode: code, verificationCodeExpires: expiresAt });
  }

  async markEmailAsVerified(userId: string): Promise<void> {
    await this.userModel.updateOne({ _id: userId }, { emailVerified: true, verificationCode: null, verificationCodeExpires: null });
  }

  async linkChild(parentId: string, childId: string): Promise<UserDocument> {
    const parent = await this.userModel.findById(parentId);
    const child = await this.userModel.findById(childId);
    if (!parent) throw new NotFoundException('Parent non trouvé');
    if (!child) throw new NotFoundException('Enfant non trouvé');
    if (parent.role !== UserRole.PARENT) throw new BadRequestException('L\'utilisateur doit être un parent');
    if (child.role !== UserRole.ENFANT) throw new BadRequestException('L\'utilisateur doit être un enfant');
    if (child.parent) throw new ConflictException('Cet enfant a déjà un parent');

    child.parent = parent._id;
    await child.save();

    if (!parent.enfants) parent.enfants = [];
    if (!parent.enfants.some(id => id.toString() === childId)) {
      parent.enfants.push(child._id);
      await parent.save();
    }
    return this.userModel.findById(parentId).populate('enfants').populate('parent').exec() as any;
  }

  async getChildren(parentId: string): Promise<UserDocument[]> {
    const children = await this.userModel.find({
      role: UserRole.ENFANT,
      $or: [{ parent: parentId }, { createdBy: parentId }],
    }).exec();
    return children;
  }

  async findAllChildrenCompact(): Promise<Array<{ _id: string; prenom: string; nom: string; fullName: string }>> {
    const enfants = await this.userModel.find({ role: UserRole.ENFANT }).select('prenom nom').exec();
    return enfants.map((e: any) => ({
      _id: e._id.toString(),
      prenom: e.prenom,
      nom: e.nom,
      fullName: `${e.prenom ?? ''} ${e.nom ?? ''}`.trim()
    }));
  }

  async findChildrenCompactByParent(parentId: string): Promise<Array<{ _id: string; prenom: string; nom: string; fullName: string }>> {
    const queryParentId = Types.ObjectId.isValid(parentId) ? new Types.ObjectId(parentId) : parentId;
    const enfants = await this.userModel.find({
      role: UserRole.ENFANT,
      $or: [{ parent: queryParentId }, { createdBy: queryParentId }],
    }).select('prenom nom').exec();
    return enfants.map((e: any) => ({
      _id: e._id.toString(),
      prenom: e.prenom,
      nom: e.nom,
      fullName: `${e.prenom ?? ''} ${e.nom ?? ''}`.trim()
    }));
  }

  async createChild(parentId: string, createChildDto: CreateChildDto): Promise<UserDocument> {
    const parent = await this.userModel.findById(parentId);
    if (!parent) throw new NotFoundException('Parent non trouvé');
    if (parent.role !== UserRole.PARENT) throw new BadRequestException('L\'utilisateur doit être un parent');

    const timestamp = Date.now();
    const childData = {
      prenom: createChildDto.prenom,
      nom: createChildDto.nom,
      dateNaissance: new Date(createChildDto.dateNaissance),
      sportPratique: createChildDto.sportPratique,
      role: UserRole.ENFANT,
      parent: parent._id,
      photoProfil: createChildDto.photoProfil,
      email: `enfant_${parent._id}_${timestamp}@academie.local`,
      motDePasse: await bcrypt.hash(`temp_${timestamp}`, 10)
    };

    const child = new this.userModel(childData);
    await child.save();

    if (!parent.enfants) parent.enfants = [];
    parent.enfants.push(child._id);
    await parent.save();

    return this.userModel.findById(parentId).populate('enfants').populate('parent').exec() as any;
  }

  async getChildData(parentId: string, childId: string): Promise<UserDocument> {
    const child = await this.userModel.findById(childId);
    if (!child) throw new NotFoundException('Enfant non trouvé');
    if (child.role !== UserRole.ENFANT) throw new BadRequestException('L\'utilisateur doit être un enfant');
    if (!child.parent || child.parent.toString() !== parentId) {
      throw new BadRequestException('Cet enfant n\'appartient pas à ce parent');
    }
    return child;
  }

  async findChildByName(prenom: string, nom: string): Promise<UserDocument | null> {
    return this.userModel.findOne({
      role: UserRole.ENFANT,
      prenom: new RegExp(`^${prenom}$`, 'i'),
      nom: new RegExp(`^${nom}$`, 'i'),
    }).exec();
  }

  /**
   * Récupère tous les enfants (joueurs) pour un coach.
   * Note: Dans cette implémentation, le coach peut voir tous les enfants du système.
   * @param coachId ID du coach
   * @returns Liste des enfants
   */
  async getChildrenOfCoach(coachId: string): Promise<UserDocument[]> {
    this.logger.debug(`Fetching children for coach ${coachId}`);
    return this.userModel.find({ role: UserRole.ENFANT }).exec();
  }
}


