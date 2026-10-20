import {
  Directive,
  inject,
  Input,
  TemplateRef,
  ViewContainerRef
} from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Directiva estructural para mostrar u ocultar elementos del DOM en función
 * de los roles asignados al usuario en el token JWT de Keycloak.
 *
 * Uso en templates:
 * ```html
 * <button *hasRole="'ADMIN'">Eliminar Producto</button>
 * <button *hasRole="['ADMIN', 'JEFE_PRODUCCION']">Nueva Orden</button>
 * ```
 */
@Directive({
  selector: '[hasRole]',
  standalone: true
})
export class HasRoleDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly authService = inject(AuthService);

  private hasView = false;

  @Input()
  set hasRole(roles: string | string[] | null | undefined) {
    if (!roles) {
      this.clearView();
      return;
    }

    // Normaliza roles a un arreglo
    const rolesArray = Array.isArray(roles) ? roles : [roles];
    const isAuthorized = this.authService.hasAnyRole(rolesArray);

    if (isAuthorized && !this.hasView) {
      // Si está autorizado y el elemento no está en el DOM, se crea la vista
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!isAuthorized && this.hasView) {
      // Si no está autorizado y el elemento estaba en el DOM, se destruye
      this.clearView();
    }
  }

  private clearView(): void {
    this.viewContainer.clear();
    this.hasView = false;
  }
}
