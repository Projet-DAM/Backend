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
  ) { }

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
    this.logger.log(`📧 === DÉBUT ENVOI EMAIL DE VÉRIFICATION ===`);
    this.logger.log(`📧 Email: ${user.email}, Nom: ${user.nom}, Prénom: ${user.prenom}, Code: ${verificationCode}`);
    this.logger.log(`📧 EmailService injecté: ${this.emailService ? 'OUI' : 'NON'}`);
    
    if (!this.emailService) {
      this.logger.error('❌ ERREUR CRITIQUE: EmailService n\'est pas injecté!');
      throw new Error('EmailService non disponible');
    }
    
    try {
      this.logger.log(`📧 Appel de emailService.sendVerificationCode...`);
      await this.emailService.sendVerificationCode(
        user.email,
        user.nom,
        user.prenom,
        verificationCode,
      );
      this.logger.log(`✅ Email envoyé avec succès`);
    } catch (error: any) {
      this.logger.error('❌ === ERREUR ENVOI EMAIL ===');
      this.logger.error('❌ Message:', error?.message || error);
      this.logger.error('❌ Stack:', error?.stack);
      this.logger.error('❌ Code:', error?.code);
      this.logger.error('❌ Type:', error?.constructor?.name);
      this.logger.error('❌ Response:', error?.response);
      this.logger.error('❌ Command:', error?.command);
      // NE PAS continuer silencieusement - lancer l'erreur pour voir ce qui se passe
      // throw error; // Décommenter pour forcer l'erreur à remonter
    }
    this.logger.log(`📧 === FIN ENVOI EMAIL ===`);

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

  async testEmail(email: string, nom: string, prenom: string) {
    this.logger.log(`🧪 === TEST ENVOI EMAIL ===`);
    this.logger.log(`🧪 Email: ${email}, Nom: ${nom}, Prénom: ${prenom}`);
    
    const testCode = '123456';
    try {
      await this.emailService.sendVerificationCode(email, nom, prenom, testCode);
      this.logger.log(`✅ Test email réussi`);
      return {
        success: true,
        message: `Email de test envoyé avec succès à ${email}`,
        code: testCode,
      };
    } catch (error: any) {
      this.logger.error(`❌ Test email échoué:`, error);
      return {
        success: false,
        message: `Erreur lors de l'envoi de l'email de test: ${error.message || error}`,
        error: error.message || error,
        stack: error.stack,
      };
    }
  }
}

