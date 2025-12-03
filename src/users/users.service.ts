import { UserRole } from './interfaces/user-role.enum';
import { Injectable, NotFoundException, ConflictException, BadRequestException, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import * as bcrypt from 'bcrypt';
import { User, UserDocument } from './entity/user.entity';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { CreateChildDto } from './dto/create-child.dto';

@Injectable()
export class UsersService {
  private readonly logger = new Logger(UsersService.name);

  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
  ) {}

  async removeChild(childId: string, parentId: string): Promise<{ success: boolean; childId: string }> {
    if (!Types.ObjectId.isValid(childId)) {
      throw new BadRequestException('childId invalide');
    }
    if (!Types.ObjectId.isValid(parentId)) {
      throw new BadRequestException('parentId invalide');
    }
    // Vérifier que l'enfant existe et appartient bien au parent
    const child = await this.userModel.findById(childId);
    if (!child) {
      throw new NotFoundException("Enfant non trouvé");
    }
    if (child.role !== UserRole.ENFANT) {
      throw new BadRequestException("L'utilisateur n'est pas un enfant");
    }
    // Autoriser la suppression si le demandeur est le parent lié OU si c'est une académie
    let isAuthorized = false;
    // Parent lié ?
    if (child.parent && child.parent.toString() === parentId) {
      isAuthorized = true;
    } else if ('createdBy' in child && child['createdBy'] && child['createdBy'].toString() === parentId) {
      isAuthorized = true;
    }
    // Si le parentId correspond à un utilisateur de rôle ACADEMIE, autoriser aussi
    const requester = await this.userModel.findById(parentId);
    if (requester && requester.role === UserRole.ACADEMIE) {
      isAuthorized = true;
    }
    if (!isAuthorized) {
      throw new BadRequestException("Non autorisé à supprimer cet enfant");
    }
    // Supprimer l'enfant de la liste des enfants du parent
    await this.userModel.updateOne(
      { _id: parentId },
      { $pull: { enfants: child._id } }
    );
    // Supprimer l'enfant de la base
    await this.userModel.findByIdAndDelete(childId);
    return { success: true, childId };
  }

  async create(createUserDto: CreateUserDto): Promise<UserDocument> {
    // Vérifier si l'email existe déjà (sauf pour les enfants avec email temporaire)
    if (createUserDto.email && !createUserDto.email.includes('@temp.com')) {
      const existingUser = await this.userModel.findOne({ email: createUserDto.email });
      if (existingUser) {
        throw new ConflictException('Cet email est déjà utilisé');
      }
    }

    // Hasher le mot de passe (ou générer un mot de passe temporaire pour les enfants)
    const passwordToHash = createUserDto.motDePasse || 'temp123';
    const hashedPassword = await bcrypt.hash(passwordToHash, 10);

    // Validation logique selon le rôle
    const userData: any = {
      ...createUserDto,
      motDePasse: hashedPassword,
    };

    // Convertir dateNaissance de string à Date si présente
    if (createUserDto.dateNaissance) {
      userData.dateNaissance = new Date(createUserDto.dateNaissance);
    }

    if (createUserDto.role === UserRole.ENFANT) {
      // Un enfant ne peut pas avoir d'attribut enfants, mais peut avoir un parent et un coach
      delete userData.enfants;
      // Supprimer les attributs spécifiques aux autres rôles
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;
      
      // Gérer le parentId si fourni
      if (createUserDto.parentId) {
        const parent = await this.userModel.findById(createUserDto.parentId);
        if (!parent) {
          throw new NotFoundException('Parent non trouvé');
        }
        if (parent.role !== UserRole.PARENT) {
          throw new BadRequestException('L\'utilisateur spécifié comme parent doit avoir le rôle parent');
        }
        // Convertir parentId (string) en ObjectId
        userData.parent = new Types.ObjectId(createUserDto.parentId);
        delete userData.parentId; // Supprimer parentId car on utilise parent (ObjectId)
      }
    } else if (createUserDto.role === UserRole.PARENT) {
      // Un parent peut avoir des enfants, mais ne peut pas avoir d'attribut parent
      delete userData.parent;
      delete userData.coach;
      // Supprimer les attributs spécifiques aux autres rôles
      delete userData.dateNaissance;
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;
    } else if (createUserDto.role === UserRole.COACH) {
      // Un coach ne peut pas avoir d'attributs enfants/parent
      delete userData.enfants;
      delete userData.parent;
      delete userData.coach;
      // Supprimer les attributs spécifiques aux autres rôles
      delete userData.dateNaissance;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;
    } else if (createUserDto.role === UserRole.ACADEMIE) {
      // Une académie ne peut pas avoir d'attributs enfants/parent
      delete userData.enfants;
      delete userData.parent;
      delete userData.coach;
      // Supprimer les attributs spécifiques aux autres rôles
      delete userData.dateNaissance;
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
    } else {
      // Pour les autres rôles, aucun des attributs spécifiques
      delete userData.enfants;
      delete userData.parent;
      delete userData.coach;
    }

    const user = new this.userModel(userData);
    const savedUser = await user.save();

    // Si c'est un enfant avec un parent, ajouter l'enfant à la liste des enfants du parent
    if (createUserDto.role === UserRole.ENFANT && savedUser.parent) {
      const parent = await this.userModel.findById(savedUser.parent);
      if (parent) {
        if (!parent.enfants) {
          parent.enfants = [];
        }
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
    
    if (filters?.role) {
      query.role = filters.role;
    }
    
    if (filters?.parentId) {
      // Convertir parentId (string) en ObjectId pour la requête MongoDB
      try {
        query.parent = new Types.ObjectId(filters.parentId);
        console.log(`[UsersService] findAll - Filtrage par parentId: ${filters.parentId} (ObjectId: ${query.parent})`);
      } catch (error) {
        console.error(`[UsersService] Erreur lors de la conversion du parentId en ObjectId: ${error}`);
        throw new BadRequestException(`ID parent invalide: ${filters.parentId}`);
      }
    }
    
    console.log(`[UsersService] findAll - Query:`, JSON.stringify(query));
    const users = await this.userModel.find(query).populate('enfants').populate('parent').exec();
    console.log(`[UsersService] findAll - Résultat: ${users.length} utilisateur(s) trouvé(s)`);
    if (filters?.role === UserRole.ENFANT && users.length > 0) {
      console.log(`[UsersService] Détails des enfants:`, users.map(u => ({ 
        id: u._id?.toString(), 
        nom: u.nom, 
        prenom: u.prenom, 
        parent: u.parent?.toString() || u.parent 
      })));
    }
    return users;
  }

  async findById(id: string): Promise<UserDocument | null> {
    return this.userModel
      .findById(id)
      .populate('enfants')
      .populate('parent')
      .populate('coach')
      .exec();
  }

  async findByEmail(email: string): Promise<UserDocument | null> {
    return this.userModel.findOne({ email }).exec();
  }

  async update(id: string, updateUserDto: UpdateUserDto): Promise<UserDocument> {
    const user = await this.userModel.findById(id);
    if (!user) {
      throw new NotFoundException('Utilisateur non trouvé');
    }

    // Si l'email est modifié, vérifier qu'il n'existe pas déjà
    if (updateUserDto.email && updateUserDto.email !== user.email) {
      const existingUser = await this.userModel.findOne({ email: updateUserDto.email });
      if (existingUser) {
        throw new ConflictException('Cet email est déjà utilisé');
      }
    }

    // Déterminer le rôle final (nouveau rôle ou rôle actuel)
    const finalRole = updateUserDto.role || user.role;

    // Convertir dateNaissance de string à Date si présente
    if (updateUserDto.dateNaissance) {
      (updateUserDto as any).dateNaissance = new Date(updateUserDto.dateNaissance);
    }

    // Validation logique selon le rôle
    if (finalRole === UserRole.ENFANT) {
      // Un enfant ne peut pas avoir d'attribut enfants
      if ('enfants' in updateUserDto) {
        throw new BadRequestException('Un enfant ne peut pas avoir d\'attribut enfants');
      }
      // S'assurer que l'enfant n'a pas d'enfants dans la base
      if (user.enfants && user.enfants.length > 0) {
        user.enfants = [];
      }
      // Supprimer les attributs spécifiques aux autres rôles
      if ('certification' in updateUserDto || 'specialite' in updateUserDto || 'experience' in updateUserDto) {
        throw new BadRequestException('Un enfant ne peut pas avoir d\'attributs de coach');
      }
      if ('nomAcademie' in updateUserDto || 'adresse' in updateUserDto || 'description' in updateUserDto || 'horaires' in updateUserDto) {
        throw new BadRequestException('Un enfant ne peut pas avoir d\'attributs d\'académie');
      }
      // Un enfant peut avoir un parent (géré via linkChild) et un coach
    } else if (finalRole === UserRole.PARENT) {
      // Un parent peut avoir des enfants (géré via linkChild), mais ne peut pas avoir d'attribut parent
      if ('parent' in updateUserDto) {
        throw new BadRequestException('Un parent ne peut pas avoir d\'attribut parent');
      }
      if ('coach' in updateUserDto) {
        throw new BadRequestException('Un parent ne peut pas avoir de coach');
      }
      // S'assurer que le parent n'a pas de parent dans la base
      if (user.parent) {
        user.parent = undefined;
      }
      // Supprimer les attributs spécifiques aux autres rôles
      if ('dateNaissance' in updateUserDto) {
        throw new BadRequestException('Un parent ne peut pas avoir de date de naissance');
      }
      if ('certification' in updateUserDto || 'specialite' in updateUserDto || 'experience' in updateUserDto) {
        throw new BadRequestException('Un parent ne peut pas avoir d\'attributs de coach');
      }
      if ('nomAcademie' in updateUserDto || 'adresse' in updateUserDto || 'description' in updateUserDto || 'horaires' in updateUserDto) {
        throw new BadRequestException('Un parent ne peut pas avoir d\'attributs d\'académie');
      }
    } else if (finalRole === UserRole.COACH) {
      // Un coach ne peut pas avoir d'attributs enfants/parent
      if ('enfants' in updateUserDto) {
        throw new BadRequestException('Un coach ne peut pas avoir d\'attribut enfants');
      }
      if ('parent' in updateUserDto) {
        throw new BadRequestException('Un coach ne peut pas avoir d\'attribut parent');
      }
      if ('coach' in updateUserDto) {
        throw new BadRequestException('Un coach ne peut pas avoir de coach');
      }
      if (user.enfants && user.enfants.length > 0) {
        user.enfants = [];
      }
      if (user.parent) {
        user.parent = undefined;
      }
      // Supprimer les attributs spécifiques aux autres rôles
      if ('dateNaissance' in updateUserDto) {
        throw new BadRequestException('Un coach ne peut pas avoir de date de naissance');
      }
      if ('nomAcademie' in updateUserDto || 'adresse' in updateUserDto || 'description' in updateUserDto || 'horaires' in updateUserDto) {
        throw new BadRequestException('Un coach ne peut pas avoir d\'attributs d\'académie');
      }
    } else if (finalRole === UserRole.ACADEMIE) {
      // Une académie ne peut pas avoir d'attributs enfants/parent
      if ('enfants' in updateUserDto) {
        throw new BadRequestException('Une académie ne peut pas avoir d\'attribut enfants');
      }
      if ('parent' in updateUserDto) {
        throw new BadRequestException('Une académie ne peut pas avoir d\'attribut parent');
      }
      if ('coach' in updateUserDto) {
        throw new BadRequestException('Une académie ne peut pas avoir de coach');
      }
      if (user.enfants && user.enfants.length > 0) {
        user.enfants = [];
      }
      if (user.parent) {
        user.parent = undefined;
      }
      // Supprimer les attributs spécifiques aux autres rôles
      if ('dateNaissance' in updateUserDto) {
        throw new BadRequestException('Une académie ne peut pas avoir de date de naissance');
      }
      if ('certification' in updateUserDto || 'specialite' in updateUserDto || 'experience' in updateUserDto) {
        throw new BadRequestException('Une académie ne peut pas avoir d\'attributs de coach');
      }
    } else {
      // Pour les autres rôles, aucun des attributs spécifiques
      if ('enfants' in updateUserDto) {
        throw new BadRequestException('Ce rôle ne peut pas avoir d\'attribut enfants');
      }
      if ('parent' in updateUserDto) {
        throw new BadRequestException('Ce rôle ne peut pas avoir d\'attribut parent');
      }
      if ('coach' in updateUserDto) {
        throw new BadRequestException('Ce rôle ne peut pas avoir de coach');
      }
      if (user.enfants && user.enfants.length > 0) {
        user.enfants = [];
      }
      if (user.parent) {
        user.parent = undefined;
      }
    }

    // Si le mot de passe est modifié, le hasher
    if (updateUserDto.motDePasse) {
      updateUserDto.motDePasse = await bcrypt.hash(updateUserDto.motDePasse, 10);
    }

    Object.assign(user, updateUserDto);
    return user.save();
  }

  async remove(id: string): Promise<void> {
    const cleanId = id && typeof id === 'string' ? id.trim() : id;
    if (!Types.ObjectId.isValid(cleanId)) {
      throw new BadRequestException('id invalide');
    }

    const user = await this.userModel.findById(cleanId);
    if (!user) {
      throw new NotFoundException('Utilisateur non trouvé');
    }

    // Si c'est un enfant, le retirer de la liste des enfants du parent
    if (user.role === UserRole.ENFANT && user.parent) {
      const parent = await this.userModel.findById(user.parent);
      if (parent && parent.enfants) {
        parent.enfants = parent.enfants.filter(
          (childId: any) => childId.toString() !== cleanId,
        );
        await parent.save();
      }
    }

    await this.userModel.findByIdAndDelete(cleanId);
  }

  async linkChild(parentId: string, childId: string): Promise<UserDocument> {
    const parent = await this.userModel.findById(parentId);
    const child = await this.userModel.findById(childId);

    if (!parent) {
      throw new NotFoundException('Parent non trouvé');
    }

    if (!child) {
      throw new NotFoundException('Enfant non trouvé');
    }

    if (parent.role !== UserRole.PARENT) {
      throw new BadRequestException('L\'utilisateur doit être un parent');
    }

    if (child.role !== UserRole.ENFANT) {
      throw new BadRequestException('L\'utilisateur doit être un enfant');
    }

    // Vérifier si l'enfant n'a pas déjà un parent
    if (child.parent) {
      throw new ConflictException('Cet enfant a déjà un parent');
    }

    // Lier l'enfant au parent
    child.parent = parent._id;
    await child.save();

    // Ajouter l'enfant à la liste des enfants du parent
    if (!parent.enfants) {
      parent.enfants = [];
    }
    if (!parent.enfants.some(id => id.toString() === childId)) {
      parent.enfants.push(child._id);
      await parent.save();
    }

    const updatedParent = await this.userModel.findById(parentId).populate('enfants').populate('parent').exec();
    if (!updatedParent) {
      throw new NotFoundException('Parent non trouvé après mise à jour');
    }
    return updatedParent;
  }

  async getChildren(parentId: string): Promise<UserDocument[]> {
    this.logger.debug(`getChildren called for parentId=${parentId}`);
    const parent = await this.userModel.findById(parentId).exec();
    if (!parent) {
      throw new NotFoundException('Parent non trouvé');
    }

    if (parent.role !== UserRole.PARENT) {
      throw new BadRequestException('L\'utilisateur doit être un parent');
    }

    // Find children linked either by `parent` field or by `createdBy` (parent-created)
    const queryParentId = Types.ObjectId.isValid(parentId) ? new Types.ObjectId(parentId) : parentId;
    const children = await this.userModel.find({
      $and: [
        { role: UserRole.ENFANT },
        { $or: [{ parent: queryParentId }, { createdBy: queryParentId }] },
      ],
    }).exec();
    this.logger.debug(`getChildren result count=${children.length} for parentId=${parentId}`);
    return children;
  }

  /**
   * Return a compact list of all children for dropdowns (id, prenom, nom, fullName)
   */
  async findAllChildrenCompact(): Promise<Array<{ _id: string; prenom: string; nom: string; fullName: string }>> {
    const enfants = await this.userModel.find({ role: UserRole.ENFANT }).select('prenom nom').exec();
    return enfants.map((e: any) => ({ _id: e._id.toString(), prenom: e.prenom, nom: e.nom, fullName: `${e.prenom ?? ''} ${e.nom ?? ''}`.trim() }));
  }

  /**
   * Return a compact list of all children including owner fields so callers
   * can compute `ownedByRequester` without additional DB round-trips.
   */
  async findAllChildrenCompactWithOwners(): Promise<Array<{ _id: string; prenom: string; nom: string; fullName: string; parent?: string; createdBy?: string }>> {
    const enfants = await this.userModel.find({ role: UserRole.ENFANT }).select('prenom nom parent createdBy').exec();
    return enfants.map((e: any) => ({
      _id: e._id.toString(),
      prenom: e.prenom,
      nom: e.nom,
      fullName: `${e.prenom ?? ''} ${e.nom ?? ''}`.trim(),
      parent: e.parent && e.parent.toString ? e.parent.toString() : e.parent,
      createdBy: e.createdBy && e.createdBy.toString ? e.createdBy.toString() : e.createdBy,
    }));
  }

  /**
   * Return a compact list of children for a specific parent.
   * Handles ObjectId vs string by normalizing the parentId when possible.
   */
  async findChildrenCompactByParent(parentId: string): Promise<Array<{ _id: string; prenom: string; nom: string; fullName: string }>> {
    const queryParentId = Types.ObjectId.isValid(parentId) ? new Types.ObjectId(parentId) : parentId;
    // select parent and createdBy for debugging and defensive filtering
    const enfants = await this.userModel.find({
      $and: [
        { role: UserRole.ENFANT },
        { $or: [{ parent: queryParentId }, { createdBy: queryParentId }] },
      ],
    }).select('prenom nom parent createdBy').exec();
    this.logger.debug(`findChildrenCompactByParent parent=${queryParentId} -> found=${enfants.length}`);
    // map including parent id so callers can defensively verify linkage
    return enfants.map((e: any) => ({ _id: e._id.toString(), prenom: e.prenom, nom: e.nom, fullName: `${e.prenom ?? ''} ${e.nom ?? ''}`.trim(), parent: e.parent && e.parent.toString ? e.parent.toString() : e.parent, createdBy: e.createdBy && e.createdBy.toString ? e.createdBy.toString() : e.createdBy }));
  }

  // Create a child user and link it to the given parent
  async createChildForParent(parentId: string, createChildDto: any): Promise<any> {
    const parent = await this.userModel.findById(parentId);
    if (!parent) {
      throw new NotFoundException('Parent non trouvé');
    }
    if (parent.role !== UserRole.PARENT) {
      throw new BadRequestException('L\'utilisateur cible doit être un parent');
    }
    // Build full CreateUserDto for the child, auto-generate email/password if not provided
    const timestamp = Date.now();
    const generatedEmail = `${parentId}-${createChildDto.prenom || 'child'}-${timestamp}@local`;
    const generatedPassword = Math.random().toString(36).slice(-8);

    const payload: any = {
      nom: createChildDto.nom,
      prenom: createChildDto.prenom,
      role: UserRole.ENFANT,
      dateNaissance: createChildDto.dateNaissance,
      photoProfil: createChildDto.photoProfil,
      email: createChildDto.email ?? generatedEmail,
      motDePasse: createChildDto.motDePasse ?? generatedPassword,
    };

    const child = await this.create(payload);

    // Link child to parent
    await this.linkChild(parentId, child._id.toString());

    return {
      child,
      generatedCredentials: {
        email: payload.email,
        motDePasse: payload.motDePasse,
      },
    };
  }

  // Récupérer les enfants d'un coach (utilisé par les coaches pour sélectionner un enfant)
  async getChildrenOfCoach(coachId: string): Promise<UserDocument[]> {
    return this.userModel.find({ coach: coachId, role: UserRole.ENFANT }).exec();
  }

  async updateVerificationCode(userId: string, code: string, expiresAt: Date): Promise<void> {
    await this.userModel.findByIdAndUpdate(userId, {
      verificationCode: code,
      verificationCodeExpires: expiresAt,
    });
  }

  async markEmailAsVerified(userId: string): Promise<void> {
    await this.userModel.findByIdAndUpdate(userId, {
      emailVerified: true,
      verificationCode: undefined,
      verificationCodeExpires: undefined,
    });
  }

  async getChildData(parentId: string, childId: string): Promise<UserDocument> {
    const parent = await this.userModel.findById(parentId);
    if (!parent) {
      throw new NotFoundException('Parent non trouvé');
    }

    if (parent.role !== UserRole.PARENT) {
      throw new BadRequestException('L\'utilisateur doit être un parent');
    }

    const child = await this.userModel.findById(childId);
    if (!child) {
      throw new NotFoundException('Enfant non trouvé');
    }

    if (child.role !== UserRole.ENFANT) {
      throw new BadRequestException('L\'utilisateur doit être un enfant');
    }

    // Vérifier que l'enfant appartient bien au parent
    if (!child.parent || child.parent.toString() !== parentId) {
      throw new BadRequestException('Cet enfant n\'appartient pas à ce parent');
    }

    // Vérifier que l'enfant est dans la liste des enfants du parent
    if (!parent.enfants || !parent.enfants.some(id => id.toString() === childId)) {
      throw new BadRequestException('Cet enfant n\'appartient pas à ce parent');
    }

    return child;
  }
}

