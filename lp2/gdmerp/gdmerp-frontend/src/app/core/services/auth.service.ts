import { computed, inject, Injectable, signal } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly keycloak = inject(KeycloakService);

  // Inicializamos los signals vacíos para no bloquear la renderización de Angular
  readonly username = signal<string>('');
  readonly userRoles = signal<string[]>([]);
  readonly isAuthenticated = signal<boolean>(false);

  // Alias reactivos para compatibilidad con módulos de negocio (ventas, finanzas)
  readonly roles = this.userRoles;
  readonly usuario = computed(() => {
    const un = this.username();
    return {
      nombre: un || 'Usuario',
      username: un || '',
      email: ''
    };
  });

  constructor() {
    this.initSignalsSeguro();
  }

  /**
   * Carga los datos de Keycloak de forma segura atrapando errores prematuros.
   */
  private initSignalsSeguro() {
    try {
      const isAuth = this.keycloak.isLoggedIn();
      this.isAuthenticated.set(isAuth);

      if (isAuth) {
        this.userRoles.set(this.keycloak.getUserRoles(true));
        try {
          this.username.set(this.keycloak.getUsername());
        } catch (error) {
          // Si el perfil no ha cargado completamente, lo dejamos vacío por ahora
          console.warn('Perfil de Keycloak aún cargando en segundo plano...');
        }
      }
    } catch (e) {
      console.error('Error al inicializar estado de Auth:', e);
    }
  }

  /**
   * Indica de forma síncrona si el usuario tiene una sesión activa en Keycloak.
   */
  isLoggedIn(): boolean {
    try {
      return this.keycloak.isLoggedIn();
    } catch (e) {
      return false;
    }
  }

  /**
   * Retorna el username del usuario autenticado extraído del token JWT.
   */
  getUsername(): string {
    try {
      return this.isLoggedIn() ? this.keycloak.getUsername() : '';
    } catch (error) {
      return '';
    }
  }

  /**
   * Retorna el listado completo de roles asignados al usuario.
   */
  getUserRoles(): string[] {
    try {
      return this.isLoggedIn() ? this.keycloak.getUserRoles(true) : [];
    } catch (error) {
      return [];
    }
  }

  /**
   * Verifica si el usuario posee un rol específico.
   *
   * @param role Nombre del rol a consultar (ej. 'ADMIN', 'JEFE_PRODUCCION')
   */
  hasRole(role: string): boolean {
    if (!this.isLoggedIn() || !role) {
      return false;
    }
    return this.keycloak.isUserInRole(role) || this.getUserRoles().includes(role);
  }

  /**
   * Verifica si el usuario posee al menos uno de los roles indicados en el arreglo.
   *
   * @param roles Arreglo de roles candidatos
   */
  hasAnyRole(roles: string[]): boolean {
    if (!this.isLoggedIn() || !roles || roles.length === 0) {
      return false;
    }
    return roles.some((role) => this.hasRole(role));
  }

  /**
   * Retorna el identificador numérico de usuario (fallback para compatibilidad POS).
   */
  usuarioId(): number {
    return 1;
  }

  /**
   * Verifica roles permitidos (alias de hasAnyRole).
   */
  tieneRol(...rolesPermitidos: string[]): boolean {
    return this.hasAnyRole(rolesPermitidos);
  }

  /**
   * Inicia el flujo de autenticación (Login) en Keycloak.
   */
  login(redirectUri?: string): Promise<void> {
    return this.keycloak.login({
      redirectUri: redirectUri || window.location.origin
    });
  }

  /**
   * Cierra la sesión en Keycloak y redirige al origen de la app.
   */
  logout(redirectUri?: string): Promise<void> {
    return this.keycloak.logout(redirectUri || window.location.origin);
  }
}