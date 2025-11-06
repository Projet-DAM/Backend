import { Injectable, ExecutionContext, UnauthorizedException, ForbiddenException } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { Reflector } from '@nestjs/core';
import { ROLES_KEY } from '../../common/decorators/roles.decorator';
import { IS_PUBLIC_KEY } from '../../common/decorators/public.decorator';
import { UserRole } from '../../users/interfaces/user-role.enum';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  constructor(private reflector: Reflector) {
    super();
  }

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (isPublic) {
      return true;
    }

    const requiredRoles = this.reflector.getAllAndOverride<UserRole[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    try {
      const isAuthenticated = await super.canActivate(context) as boolean;
      
      if (!isAuthenticated) {
        throw new UnauthorizedException('Token invalide ou expiré. Vérifiez que le token est correctement envoyé dans le header Authorization: Bearer <token>');
      }

      if (!requiredRoles) {
        return true;
      }

      const request = context.switchToHttp().getRequest();
      const user = request.user;

      if (!user) {
        throw new UnauthorizedException('Utilisateur non authentifié. Le token n\'a pas pu être validé.');
      }

      const hasRole = requiredRoles.some((role) => user.role === role);
      
      if (!hasRole) {
        throw new ForbiddenException(`Accès refusé : rôle insuffisant. Rôle requis: ${requiredRoles.join(' ou ')}, Rôle actuel: ${user.role}`);
      }
    } catch (error) {
      if (error instanceof UnauthorizedException || error instanceof ForbiddenException) {
        throw error;
      }
      throw new UnauthorizedException(`Erreur d'authentification: ${error.message}`);
    }

    return true;
  }
}

