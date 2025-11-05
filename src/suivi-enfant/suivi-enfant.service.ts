import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { SuiviEnfant, SuiviEnfantDocument } from './suivi-enfant.schema';
import { CreateSuiviEnfantDto } from './dto/create-suivi-enfant.dto';
import { UpdateSuiviEnfantDto } from './dto/update-suivi-enfant.dto';
import { UsersService } from '../users/users.service';
import { UserRole } from '../users/interfaces/user-role.enum';

@Injectable()
export class SuiviEnfantService {
  constructor(
    @InjectModel(SuiviEnfant.name)
    private readonly suiviModel: Model<SuiviEnfantDocument>,
    private readonly usersService: UsersService
  ) {}

  async create(dto: CreateSuiviEnfantDto): Promise<SuiviEnfant> {
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

    const suivi = await this.suiviModel.create({
      date_suivi: dto.date_suivi,
      presence: dto.presence,
      performance: dto.performance,
      commentaire: dto.commentaire,
      enfant: new Types.ObjectId(dto.enfantId),
    });
    return suivi;
  }

  async findAll(): Promise<SuiviEnfant[]> {
    return this.suiviModel.find().populate('enfant').exec();
  }

  async findOne(id: string): Promise<SuiviEnfant> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const suivi = await this.suiviModel.findById(id).populate('enfant').exec();
    if (!suivi) throw new NotFoundException('Suivi non trouvé');
    return suivi;
  }

  async findByEnfant(enfantId: string): Promise<SuiviEnfant[]> {
    if (!Types.ObjectId.isValid(enfantId)) throw new BadRequestException('enfantId invalide');
    const enfant = await this.usersService.findById(enfantId);
    if (!enfant) {
      throw new NotFoundException('Enfant non trouvé');
    }
    return this.suiviModel.find({ enfant: new Types.ObjectId(enfantId) }).populate('enfant').exec();
  }

  async update(id: string, dto: UpdateSuiviEnfantDto): Promise<SuiviEnfant> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const suivi = await this.suiviModel.findByIdAndUpdate(id, dto, { new: true }).exec();
    if (!suivi) throw new NotFoundException('Suivi non trouvé');
    return suivi;
  }

  async remove(id: string): Promise<void> {
    if (!Types.ObjectId.isValid(id)) throw new BadRequestException('id invalide');
    const res = await this.suiviModel.findByIdAndDelete(id).exec();
    if (!res) throw new NotFoundException('Suivi non trouvé');
  }
}
