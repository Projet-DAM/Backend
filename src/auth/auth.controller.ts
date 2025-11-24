import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBody,
} from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { LoginUserDto } from '../users/dto/login-user.dto';
import { CreateUserDto } from '../users/dto/create-user.dto';
import { Public } from '../common/decorators/public.decorator';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @Public()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Inscription d\'un nouvel utilisateur' })
  @ApiBody({
    type: CreateUserDto,
    examples: {
      parentRegistration: {
        summary: 'Register as a parent',
        value: {
          nom: 'Durand',
          prenom: 'Luc',
          email: 'parent@example.com',
          motDePasse: 'parentpass',
          role: 'parent'
        }
      },
      coachRegistration: {
        summary: 'Register as a coach',
        value: {
          nom: 'Martin',
          prenom: 'Claire',
          email: 'coach@example.com',
          motDePasse: 'coachpass',
          role: 'coach',
          specialite: 'Football',
          certification: ['Certification FIFA']
        }
      }
    }
  })
  @ApiResponse({
    status: 201,
    description: 'Utilisateur créé et authentifié avec succès',
    schema: {
      example: {
        access_token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        user: {
          id: '507f1f77bcf86cd799439011',
          email: 'jean.dupont@example.com',
          nom: 'Dupont',
          prenom: 'Jean',
          role: 'parent',
          photoProfil: null,
        },
      },
    },
  })
  @ApiResponse({ status: 409, description: 'Email déjà utilisé' })
  register(@Body() createUserDto: CreateUserDto) {
    return this.authService.register(createUserDto);
  }

  @Post('login')
  @Public()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Connexion d\'un utilisateur' })
  @ApiBody({
    type: LoginUserDto,
    examples: {
      parentLogin: {
        summary: 'Parent login',
        value: { email: 'parent@example.com', motDePasse: 'parentpass' }
      },
      coachLogin: {
        summary: 'Coach login',
        value: { email: 'coach@example.com', motDePasse: 'coachpass' }
      }
    }
  })
  @ApiResponse({
    status: 200,
    description: 'Connexion réussie',
    schema: {
      example: {
        access_token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
        user: {
          id: '507f1f77bcf86cd799439011',
          email: 'jean.dupont@example.com',
          nom: 'Dupont',
          prenom: 'Jean',
          role: 'parent',
          photoProfil: null,
        },
      },
    },
  })
  @ApiResponse({ status: 401, description: 'Email ou mot de passe incorrect' })
  login(@Body() loginUserDto: LoginUserDto) {
    return this.authService.login(loginUserDto);
  }
}

