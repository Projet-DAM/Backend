import { Injectable, NotFoundException, ConflictException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcrypt';
import { User, UserDocument } from './entity/user.entity';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { CreateChildDto } from './dto/create-child.dto';
import { UserRole } from './interfaces/user-role.enum';

@Injectable()
export class UsersService {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
  ) {}

  async create(createUserDto: CreateUserDto): Promise<UserDocument> {
    // Vérifier si l'email existe déjà
    const existingUser = await this.userModel.findOne({ email: createUserDto.email });
    if (existingUser) {
      throw new ConflictException('Cet email est déjà utilisé');
    }

    // Hasher le mot de passe
    const hashedPassword = await bcrypt.hash(createUserDto.motDePasse, 10);

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
      // Un enfant ne peut pas avoir d'attribut enfants, mais peut avoir un parent
      delete userData.enfants;
      // Supprimer les attributs spécifiques aux autres rôles
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
      delete userData.nomAcademie;
      delete userData.adresse;
      delete userData.description;
      delete userData.horaires;
    } else if (createUserDto.role === UserRole.PARENT) {
      // Un parent peut avoir des enfants, mais ne peut pas avoir d'attribut parent
      delete userData.parent;
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
      // Supprimer les attributs spécifiques aux autres rôles
      delete userData.dateNaissance;
      delete userData.certification;
      delete userData.specialite;
      delete userData.experience;
    } else {
      // Pour les autres rôles, aucun des attributs spécifiques
      delete userData.enfants;
      delete userData.parent;
    }

    const user = new this.userModel(userData);
    return user.save();
  }

  async findAll(role?: UserRole): Promise<UserDocument[]> {
    const query = role ? { role } : {};
    return this.userModel.find(query).populate('enfants').populate('parent').exec();
  }

  async findById(id: string): Promise<UserDocument | null> {
    return this.userModel.findById(id).populate('enfants').populate('parent').exec();
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
      // Un enfant peut avoir un parent (géré via linkChild)
    } else if (finalRole === UserRole.PARENT) {
      // Un parent peut avoir des enfants (géré via linkChild), mais ne peut pas avoir d'attribut parent
      if ('parent' in updateUserDto) {
        throw new BadRequestException('Un parent ne peut pas avoir d\'attribut parent');
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
    const result = await this.userModel.findByIdAndDelete(id);
    if (!result) {
      throw new NotFoundException('Utilisateur non trouvé');
    }
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
    const parent = await this.userModel.findById(parentId).populate('enfants').exec();
    if (!parent) {
      throw new NotFoundException('Parent non trouvé');
    }

    if (parent.role !== UserRole.PARENT) {
      throw new BadRequestException('L\'utilisateur doit être un parent');
    }

    if (!parent.enfants || parent.enfants.length === 0) {
      return [];
    }

    // Récupérer les IDs des enfants (après populate, ils peuvent être ObjectId ou UserDocument)
    const childrenIds = parent.enfants.map((child: any) => {
      return child._id ? child._id : child;
    });

    // Récupérer les enfants complets depuis la base de données
    const children = await this.userModel.find({ _id: { $in: childrenIds } }).exec();
    return children;
  }

  async createChild(parentId: string, createChildDto: CreateChildDto): Promise<UserDocument> {
    const parent = await this.userModel.findById(parentId);
    if (!parent) {
      throw new NotFoundException('Parent non trouvé');
    }

    if (parent.role !== UserRole.PARENT) {
      throw new BadRequestException('L\'utilisateur doit être un parent');
    }

    // Créer l'enfant avec le rôle ENFANT
    const childData: any = {
      prenom: createChildDto.prenom,
      nom: createChildDto.nom,
      dateNaissance: new Date(createChildDto.dateNaissance),
      sportPratique: createChildDto.sportPratique,
      role: UserRole.ENFANT,
      parent: parent._id,
    };

    // Ajouter la photo de profil si fournie
    if (createChildDto.photoProfil) {
      childData.photoProfil = createChildDto.photoProfil;
    }

    // Les enfants n'ont pas d'email ni motDePasse
    // Générer un email unique basé sur le parent et un timestamp
    const timestamp = Date.now();
    childData.email = `enfant_${parent._id}_${timestamp}@academie.local`;
    
    // Générer un mot de passe temporaire (les enfants ne se connectent pas)
    childData.motDePasse = await bcrypt.hash(`temp_${timestamp}`, 10);

    const child = new this.userModel(childData);
    await child.save();

    // Ajouter l'enfant à la liste des enfants du parent
    if (!parent.enfants) {
      parent.enfants = [];
    }
    parent.enfants.push(child._id);
    await parent.save();

    const updatedParent = await this.userModel.findById(parentId).populate('enfants').populate('parent').exec();
    if (!updatedParent) {
      throw new NotFoundException('Parent non trouvé après mise à jour');
    }
    return updatedParent;
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

