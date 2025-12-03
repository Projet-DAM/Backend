import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBody,
  ApiBadRequestResponse,
  ApiConflictResponse,
  ApiUnauthorizedResponse,
} from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { LoginUserDto } from '../users/dto/login-user.dto';
import { CreateUserDto } from '../users/dto/create-user.dto';
import { Public } from '../common/decorators/public.decorator';
import { AuthResponseDto } from './dto/auth-response.dto';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @Public()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Inscription d\'un nouvel utilisateur' })
  @ApiBody({ type: CreateUserDto })
  @ApiResponse({
    status: 201,
    description: 'Utilisateur créé et authentifié avec succès',
    type: AuthResponseDto,
  })
  @ApiBadRequestResponse({ 
    description: 'Données invalides',
    schema: {
      example: {
        statusCode: 400,
        message: ['email must be an email', 'motDePasse must be longer than or equal to 6 characters'],
        error: 'Bad Request'
      }
    }
  })
  @ApiConflictResponse({ 
    description: 'Email déjà utilisé',
    schema: {
      example: {
        statusCode: 409,
        message: 'Cet email est déjà utilisé',
        error: 'Conflict'
      }
    }
  })
  register(@Body() createUserDto: CreateUserDto) {
    return this.authService.register(createUserDto);
  }

  @Post('login')
  @Public()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Connexion d\'un utilisateur' })
  @ApiBody({ type: LoginUserDto })
  @ApiResponse({
    status: 200,
    description: 'Connexion réussie',
    type: AuthResponseDto,
  })
  @ApiBadRequestResponse({ 
    description: 'Données invalides',
    schema: {
      example: {
        statusCode: 400,
        message: ['email must be an email', 'motDePasse should not be empty'],
        error: 'Bad Request'
      }
    }
  })
  @ApiUnauthorizedResponse({ 
    description: 'Email ou mot de passe incorrect',
    schema: {
      example: {
        statusCode: 401,
        message: 'Email ou mot de passe incorrect',
        error: 'Unauthorized'
      }
    }
  })
  login(@Body() loginUserDto: LoginUserDto) {
    return this.authService.login(loginUserDto);
  }
}

