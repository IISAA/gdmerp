import { Component, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HasRoleDirective } from '../../directives/has-role.directive';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatTooltipModule,
    HasRoleDirective
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  readonly authService = inject(AuthService);

  readonly displayName = computed(() => {
    return this.authService.username() || this.authService.getUsername() || 'Juan Pérez';
  });

  readonly primaryRole = computed(() => {
    const roles = this.authService.userRoles();
    if (roles && roles.length > 0) {
      const known = ['ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO', 'OPERARIO'];
      const found = roles.find(r => known.includes(r));
      if (found) return found;
      return roles[0];
    }
    const rolesList = this.authService.getUserRoles();
    if (rolesList && rolesList.length > 0) {
      const known = ['ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO', 'OPERARIO'];
      const found = rolesList.find(r => known.includes(r));
      if (found) return found;
      return rolesList[0];
    }
    return 'ADMIN';
  });

  logout(): void {
    this.authService.logout();
  }
}
