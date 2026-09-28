import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div style="padding: 1rem;">
      <h2 style="font-size: 1.5rem; font-weight: bold; margin-bottom: 1.5rem; color: #111827;">Panel General</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1rem;">
        
        <!-- Tarjeta de acceso a Producción -->
        <div [routerLink]="['/produccion/ordenes']" style="background: white; padding: 1.5rem; border-radius: 0.75rem; box-shadow: 0 4px 6px rgba(0,0,0,0.05); cursor: pointer; transition: transform 0.2s; border: 1px solid #e5e7eb;">
          <div style="font-size: 2rem; margin-bottom: 0.5rem;">🏭</div>
          <h3 style="font-size: 1.25rem; font-weight: 600; margin: 0 0 0.5rem 0;">Tablero de Producción</h3>
          <p style="color: #6b7280; font-size: 0.875rem; margin: 0;">Planifica y monitorea las órdenes de pasta pura, chocolates y miel.</p>
        </div>

        <!-- Tarjeta futura de Inventario/Alertas -->
        <div style="background: white; padding: 1.5rem; border-radius: 0.75rem; box-shadow: 0 4px 6px rgba(0,0,0,0.05); border: 1px solid #e5e7eb;">
          <div style="font-size: 2rem; margin-bottom: 0.5rem;">⚠️</div>
          <h3 style="font-size: 1.25rem; font-weight: 600; margin: 0 0 0.5rem 0;">Alertas de Stock</h3>
          <p style="color: #6b7280; font-size: 0.875rem; margin: 0;">(Próximamente) Mermas y materias primas por debajo del stock mínimo.</p>
        </div>

      </div>
    </div>
  `
})
export class HomeComponent {}