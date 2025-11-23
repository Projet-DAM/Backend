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
    // Si une erreur est passée, la propager
    if (err) {
      throw err;
    }
    // Si l'utilisateur n'est pas trouvé, lancer une exception
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
        throw new UnauthorizedException('Token invalide ou expiré');
      }
    } catch (error) {
      // Si l'erreur est déjà une UnauthorizedException, la propager telle quelle
      if (error instanceof UnauthorizedException) {
        throw error;
      }
      // Sinon, convertir en UnauthorizedException avec un message approprié
      throw new UnauthorizedException(error?.message || 'Token invalide ou expiré');
    }

    if (!requiredRoles) {
      return true;
    }

    const request = context.switchToHttp().getRequest();
    const user = request.user;

    if (!user) {
      throw new UnauthorizedException('Utilisateur non authentifié');
    }

    const hasRole = requiredRoles.some((role) => user.role === role);
    
    if (!hasRole) {
      throw new ForbiddenException('Accès refusé : rôle insuffisant');
    }

    return true;
  }
}

