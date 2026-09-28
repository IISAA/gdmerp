import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside style="width: 250px; background: #1f2937; color: white; height: 100vh; padding: 1rem; display: flex; flex-direction: column;">
      <div style="margin-bottom: 2rem;">
        <h2 style="margin: 0; font-size: 1.5rem; font-weight: bold; color: #f59e0b;">GDMERP</h2>
        <span style="font-size: 0.8rem; color: #9ca3af;">Producción & Retail</span>
      </div>
      <nav style="display: flex; flex-direction: column; gap: 0.5rem;">
        @for (item of menuItems; track item.path) {
          <a [routerLink]="item.path" routerLinkActive="active" style="color: #d1d5db; text-decoration: none; padding: 0.75rem; border-radius: 0.375rem; display: flex; align-items: center; gap: 0.5rem;">
            <span>{{ item.icon }}</span> {{ item.label }}
          </a>
        }
      </nav>
    </aside>
  `,
  styles: [`
    .active { background-color: #374151; color: white !important; font-weight: bold; }
    a:hover { background-color: #374151; }
  `]
})
export class SidebarComponent {
  menuItems = [
    { label: 'Inicio', path: '/home', icon: '🏠' },
    { label: 'Tablero de Producción', path: '/produccion/ordenes', icon: '🏭' },
    { label: 'Inventario y Kardex', path: '/inventario', icon: '📦' },
    { label: 'Punto de Venta', path: '/ventas', icon: '💰' }
  ];
}