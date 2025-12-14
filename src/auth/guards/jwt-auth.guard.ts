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

  handleRequest(err: any, user: any, info: any, context: ExecutionContext) {
    if (err) {
      throw err;
    }
    if (!user) {
      throw new UnauthorizedException(info?.message || 'Token invalide ou expiré');
    }
    return user;
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
        throw new UnauthorizedException(
          'Token invalide ou expiré. Vérifiez que le token est correctement envoyé dans le header Authorization: Bearer <token>',
        );
      }

      if (!requiredRoles || requiredRoles.length === 0) {
        return true;
      }

      const request = context.switchToHttp().getRequest();
      const user = request.user;

      if (!user) {
        throw new UnauthorizedException(
          "Utilisateur non authentifié. Le token n'a pas pu être validé.",
        );
      }

      const hasRole = requiredRoles.some((role) => user.role === role);

      if (!hasRole) {
        throw new ForbiddenException(
          `Accès refusé : rôle insuffisant. Rôle requis: ${requiredRoles.join(' ou ')}, Rôle actuel: ${user.role}`,
        );
      }
    } catch (error) {
      // Allow standard auth exceptions to pass through
      if (error instanceof UnauthorizedException || error instanceof ForbiddenException) {
        throw error;
      }
      throw new UnauthorizedException(`Erreur d'authentification: ${error.message}`);
    }

    return true;
  }
}
