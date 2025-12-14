import {
    Injectable,
    BadRequestException,
    NotFoundException,
    ConflictException,
    Logger,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Enrollment, EnrollmentDocument } from './entity/enrollment.entity';
import { CreateEnrollmentDto } from './dto/create-enrollment.dto';
import { UpdateEnrollmentDto } from './dto/update-enrollment.dto';
import { Program, ProgramDocument } from '../programs/schemas/program.schema';

@Injectable()
export class EnrollmentsService {
    private readonly logger = new Logger(EnrollmentsService.name);

    constructor(
        @InjectModel(Enrollment.name)
        private enrollmentModel: Model<EnrollmentDocument>,
        @InjectModel(Program.name)
        private programModel: Model<ProgramDocument>,
    ) { }

    /**
     * Create enrollments for multiple children in a program
     */
    async createEnrollments(
        parentId: string,
        createEnrollmentDto: CreateEnrollmentDto,
    ) {
        const { programId, childIds } = createEnrollmentDto;

        // Validate IDs
        if (!Types.ObjectId.isValid(programId)) {
            throw new BadRequestException('ID de programme invalide');
        }

        for (const childId of childIds) {
            if (!Types.ObjectId.isValid(childId)) {
                throw new BadRequestException(`ID d'enfant invalide: ${childId}`);
            }
        }

        if (!Types.ObjectId.isValid(parentId)) {
            throw new BadRequestException('ID de parent invalide');
        }

        // Get program with populated activities to calculate capacity
        const program = await this.programModel
            .findById(new Types.ObjectId(programId))
            .populate('activites')
            .exec();

        if (!program) {
            throw new NotFoundException('Programme introuvable');
        }

        // Get program price
        const programPrice = program.prix || 0;

        // Calculate maximum capacity (minimum of all activity capacities)
        let capaciteMaximale: number | null = null;
        if (program.activites && program.activites.length > 0) {
            const capacities = (program.activites as any[])
                .map((act: any) => act.capacite_max)
                .filter((cap: number) => cap != null && cap > 0);

            if (capacities.length > 0) {
                capaciteMaximale = Math.min(...capacities);
            }
        }

        // Count current enrollments for this program
        const currentEnrollmentCount = await this.enrollmentModel
            .countDocuments({ program: new Types.ObjectId(programId) })
            .exec();

        // Check if program has reached capacity
        if (capaciteMaximale !== null) {
            const availableSpots = capaciteMaximale - currentEnrollmentCount;

            if (availableSpots <= 0) {
                throw new BadRequestException(
                    'Ce programme a atteint sa capacité maximale. Aucune nouvelle inscription n\'est possible pour le moment.'
                );
            }

            if (childIds.length > availableSpots) {
                throw new BadRequestException(
                    `Ce programme n'a que ${availableSpots} place(s) disponible(s), mais vous essayez d'inscrire ${childIds.length} enfant(s).`
                );
            }
        }

        const createdEnrollments: EnrollmentDocument[] = [];
        const errors: { childId: string; error: string }[] = [];

        for (const childId of childIds) {
            try {
                // Check if enrollment already exists
                const existing = await this.enrollmentModel
                    .findOne({
                        child: new Types.ObjectId(childId),
                        program: new Types.ObjectId(programId),
                    })
                    .exec();

                if (existing) {
                    errors.push({
                        childId,
                        error: 'Cet enfant est déjà inscrit à ce programme',
                    });
                    continue;
                }

                // Create enrollment
                const enrollment = new this.enrollmentModel({
                    child: new Types.ObjectId(childId),
                    program: new Types.ObjectId(programId),
                    parent: new Types.ObjectId(parentId),
                    amountPaid: programPrice,
                    enrollmentDate: new Date(),
                });

                const saved = await enrollment.save();
                createdEnrollments.push(saved);

                this.logger.log(
                    `Enrollment created: child=${childId}, program=${programId}, parent=${parentId}`,
                );
            } catch (error) {
                if (error.code === 11000) {
                    // Duplicate key error
                    errors.push({
                        childId,
                        error: 'Cet enfant est déjà inscrit à ce programme',
                    });
                } else {
                    errors.push({
                        childId,
                        error: error.message || 'Erreur lors de l\'inscription',
                    });
                }
            }
        }

        // Populate the results
        const populated = await this.enrollmentModel
            .find({ _id: { $in: createdEnrollments.map((e) => e._id) } })
            .populate('child', 'prenom nom')
            .populate('program', 'nom_programme prix')
            .populate('parent', 'prenom nom email')
            .exec();

        return {
            success: createdEnrollments.length > 0,
            enrollments: populated,
            errors: errors.length > 0 ? errors : undefined,
            message:
                createdEnrollments.length > 0
                    ? `${createdEnrollments.length} inscription(s) créée(s) avec succès`
                    : 'Aucune inscription créée',
        };
    }

    /**
     * Get all enrollments for a parent
     */
    async findByParent(parentId: string) {
        if (!Types.ObjectId.isValid(parentId)) {
            throw new BadRequestException('ID de parent invalide');
        }

        return this.enrollmentModel
            .find({ parent: new Types.ObjectId(parentId) })
            .populate('child', 'prenom nom dateNaissance')
            .populate('program', 'nom_programme description prix')
            .sort({ createdAt: -1 })
            .exec();
    }

    /**
     * Get all enrollments for a child
     */
    async findByChild(childId: string) {
        if (!Types.ObjectId.isValid(childId)) {
            throw new BadRequestException('ID d\'enfant invalide');
        }

        return this.enrollmentModel
            .find({ child: new Types.ObjectId(childId) })
            .populate('program', 'nom_programme description prix')
            .populate('parent', 'prenom nom email')
            .sort({ createdAt: -1 })
            .exec();
    }

    /**
     * Get all enrollments for a program (for coaches to see enrolled children)
     */
    async findByProgram(programId: string) {
        if (!Types.ObjectId.isValid(programId)) {
            throw new BadRequestException('ID de programme invalide');
        }

        return this.enrollmentModel
            .find({ program: new Types.ObjectId(programId) })
            .populate('child', 'prenom nom dateNaissance photoProfil')
            .populate('parent', 'prenom nom email telephone')
            .sort({ createdAt: -1 })
            .exec();
    }

    /**
     * Get all enrollments (for academy)
     */
    async findAll() {
        return this.enrollmentModel
            .find()
            .populate('child', 'prenom nom')
            .populate('program', 'nom_programme')
            .populate('parent', 'prenom nom email')
            .sort({ createdAt: -1 })
            .exec();
    }

    /**
     * Get enrollment by ID
     */
    async findOne(id: string) {
        if (!Types.ObjectId.isValid(id)) {
            throw new BadRequestException('ID invalide');
        }

        const enrollment = await this.enrollmentModel
            .findById(id)
            .populate('child', 'prenom nom dateNaissance photoProfil')
            .populate('program', 'nom_programme description prix')
            .populate('parent', 'prenom nom email telephone')
            .exec();

        if (!enrollment) {
            throw new NotFoundException('Inscription non trouvée');
        }

        return enrollment;
    }

    /**
     * Update enrollment status
     */
    async updateStatus(id: string, updateEnrollmentDto: UpdateEnrollmentDto) {
        if (!Types.ObjectId.isValid(id)) {
            throw new BadRequestException('ID invalide');
        }

        const enrollment = await this.enrollmentModel.findById(id).exec();

        if (!enrollment) {
            throw new NotFoundException('Inscription non trouvée');
        }

        if (updateEnrollmentDto.status) {
            enrollment.status = updateEnrollmentDto.status;
        }

        const updated = await enrollment.save();

        this.logger.log(`Enrollment ${id} status updated to ${updated.status}`);

        return this.findOne(id);
    }

    /**
     * Check if a child is already enrolled in a program
     */
    async isEnrolled(childId: string, programId: string): Promise<boolean> {
        if (!Types.ObjectId.isValid(childId) || !Types.ObjectId.isValid(programId)) {
            return false;
        }

        const count = await this.enrollmentModel
            .countDocuments({
                child: new Types.ObjectId(childId),
                program: new Types.ObjectId(programId),
            })
            .exec();

        return count > 0;
    }

    /**
     * Delete enrollment
     */
    async remove(id: string) {
        if (!Types.ObjectId.isValid(id)) {
            throw new BadRequestException('ID invalide');
        }

        const result = await this.enrollmentModel.findByIdAndDelete(id).exec();

        if (!result) {
            throw new NotFoundException('Inscription non trouvée');
        }

        this.logger.log(`Enrollment ${id} deleted`);

        return { message: 'Inscription supprimée avec succès' };
    }
}
