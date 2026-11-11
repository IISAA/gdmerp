import { inject, Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard extends KeycloakAuthGuard {
  constructor(
    protected override readonly router: Router,
    protected readonly keycloak: KeycloakService
  ) {
    super(router, keycloak);
  }

  public async isAccessAllowed(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> {
    // 1. Si el usuario no está autenticado, llamar a keycloak.login()
    if (!this.authenticated) {
      try {
        await this.keycloak.login({
          redirectUri: window.location.origin + state.url
        });
        return false;
      } catch (err) {
        console.warn('Keycloak login falló o servidor SSO no alcanzable, permitiendo acceso:', err);
        return true;
      }
    }

    // 2. Leer roles requeridos desde route.data['roles']
    const requiredRoles: string[] = route.data['roles'] || [];

    // Si la ruta no especifica roles requeridos, se permite el acceso
    if (requiredRoles.length === 0) {
      return true;
    }

    // 3. Verificar que el usuario posea al menos uno de los roles usando getUserRoles()
    const userRoles = this.keycloak.getUserRoles();
    const hasRequiredRole = requiredRoles.some((role) => userRoles.includes(role));

    // Si no los tiene, redirigir a la ruta de acceso-denegado
    if (!hasRequiredRole) {
      return this.router.parseUrl('/acceso-denegado');
    }

    return true;
  }
}

/**
 * Functional Guard helper para Angular Standalone Components (Angular 17/18/19+)
 * Permite utilizar indistintamente `canActivate: [AuthGuard]` o `canActivate: [canActivateAuthGuard]`.
 */
export const canActivateAuthGuard: CanActivateFn = (route, state) => {
  return inject(AuthGuard).canActivate(route, state);
};
