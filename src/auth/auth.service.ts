import { Injectable, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { Injectable, Logger, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { UsersService } from '../users/users.service';
import { LoginUserDto } from '../users/dto/login-user.dto';
import { CreateUserDto } from '../users/dto/create-user.dto';
import { JwtPayload } from './jwt.strategy';
import { EmailService } from '../common/services/email.service';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    private usersService: UsersService,
    private jwtService: JwtService,
    private emailService: EmailService,
  ) {}

  async validateUser(email: string, password: string): Promise<any> {
    const user = await this.usersService.findByEmail(email);
    if (user && (await bcrypt.compare(password, user.motDePasse))) {
      const { motDePasse, ...result } = user.toObject();
      return result;
    }
    return null;
  }

  async login(loginUserDto: LoginUserDto) {
    const user = await this.validateUser(loginUserDto.email, loginUserDto.motDePasse);
    if (!user) {
      throw new UnauthorizedException('Email ou mot de passe incorrect');
    }


    const payload: JwtPayload = {
      sub: user._id.toString(),
      email: user.email,
      role: user.role,
    };

    return {
      access_token: this.jwtService.sign(payload),
      user: {
        id: user._id,
        email: user.email,
        nom: user.nom,
        prenom: user.prenom,
        role: user.role,
        photoProfil: user.photoProfil,
      },
    };
  }

  async register(createUserDto: CreateUserDto) {
    // Valider que seuls les rôles parent et coach peuvent s'inscrire via cette interface
    if (createUserDto.role !== 'parent' && createUserDto.role !== 'coach') {
      throw new BadRequestException('Seuls les rôles "parent" et "coach" peuvent s\'inscrire via cette interface');
    }

    const user = await this.usersService.create(createUserDto);
    const { motDePasse, ...userWithoutPassword } = user.toObject();

    // Générer un code de vérification à 6 chiffres
    const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();
    const verificationCodeExpires = new Date();
    verificationCodeExpires.setMinutes(verificationCodeExpires.getMinutes() + 15); // Valide 15 minutes

    // Stocker le code dans la base de données
    await this.usersService.updateVerificationCode(
      user._id.toString(),
      verificationCode,
      verificationCodeExpires,
    );

    // Envoyer l'email avec le code
    try {
      await this.emailService.sendVerificationCode(
        user.email,
        user.nom,
        user.prenom,
        verificationCode,
      );
    } catch (error) {
      this.logger.error('Erreur lors de l\'envoi de l\'email:', error);
      // Continuer même si l'email échoue, l'utilisateur pourra demander un nouveau code
    }

    // Ne pas retourner de token JWT lors de l'inscription
    // L'utilisateur devra vérifier son email avant de pouvoir se connecter
    return {
      message: 'Inscription réussie. Un code de vérification a été envoyé à votre adresse email.',
      userId: user._id.toString(),
      email: user.email,
    };
  }

  async verifyEmail(userId: string, code: string) {
    const user = await this.usersService.findById(userId);
    if (!user) {
      throw new UnauthorizedException('Utilisateur non trouvé');
    }

    if (user.emailVerified) {
      throw new UnauthorizedException('Email déjà vérifié');
    }

    if (!user.verificationCode || !user.verificationCodeExpires) {
      throw new UnauthorizedException('Code de vérification invalide ou expiré');
    }

    if (new Date() > user.verificationCodeExpires) {
      throw new UnauthorizedException('Code de vérification expiré');
    }

    if (user.verificationCode !== code) {
      throw new UnauthorizedException('Code de vérification incorrect');
    }

    // Marquer l'email comme vérifié
    await this.usersService.markEmailAsVerified(userId);

    // Générer un token JWT
    const payload: JwtPayload = {
      sub: user._id.toString(),
      email: user.email,
      role: user.role,
    };

    return {
      access_token: this.jwtService.sign(payload),
      user: {
        id: user._id,
        email: user.email,
        nom: user.nom,
        prenom: user.prenom,
        role: user.role,
        photoProfil: user.photoProfil,
      },
    };
  }
}

