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

    try {
      // First check authentication (JWT validity)
      const isAuthenticated = await super.canActivate(context) as boolean;
      if (!isAuthenticated) {
        throw new UnauthorizedException('Token invalide ou expiré');
      }
    } catch (err) {
      throw new UnauthorizedException('Token invalide ou expiré');
    }

    const requiredRoles = this.reflector.getAllAndOverride<UserRole[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredRoles) {
      return true;
    const requiredRoles = this.reflector.getAllAndOverride<UserRole[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    let isAuthenticated = false;
    try {
      isAuthenticated = await super.canActivate(context) as boolean;
    } catch (error) {
      if (error instanceof UnauthorizedException) {
        throw error;
      }
      throw new UnauthorizedException(error?.message || 'Token invalide ou expiré');
    }

    if (!isAuthenticated) {
      throw new UnauthorizedException(
        'Token invalide ou expiré. Vérifiez que le token est correctement envoyé dans le header Authorization: Bearer <token>',
      );
    }

    const request = context.switchToHttp().getRequest();
    const user = request.user;

    if (!user) {
      throw new UnauthorizedException(
        "Utilisateur non authentifié. Le token n'a pas pu être validé.",
      );
    }

    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    const hasRole = requiredRoles.some((role) => user.role === role);

    if (!hasRole) {
      throw new ForbiddenException(`Accès refusé : rôle insuffisant. Rôle requis: ${requiredRoles.join(' ou ')}`);
      throw new ForbiddenException(
        `Accès refusé : rôle insuffisant. Rôle requis: ${requiredRoles.join(
          ' ou ',
        )}, Rôle actuel: ${user.role}`,
      );
    }

    return true;
  }
}

