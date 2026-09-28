import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion } from '../../models/produccion.models';
import { Router } from '@angular/router';

@Component({
  selector: 'app-orden-list',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  template: `
    <div style="padding: 1.5rem; max-width: 1200px; margin: 0 auto;">
      <!-- Header de la sección -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
        <div>
          <h2 style="font-size: 1.75rem; font-weight: 700; color: #111827; margin: 0;">
            Órdenes de Producción
          </h2>
          <p style="color: #6b7280; font-size: 0.95rem; margin: 0.25rem 0 0 0;">
            Gestión y seguimiento de lotes de manufactura de cacao y derivados
          </p>
        </div>
        
        <a 
          routerLink="nueva"
          style="background-color: #d97706; color: white; border: none; padding: 0.625rem 1.25rem; border-radius: 0.5rem; font-weight: 600; font-size: 0.95rem; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 0.5rem; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); transition: background-color 0.2s;">
          <span>+</span> Nueva Orden
        </a>
      </div>

      <!-- Tabla de Órdenes -->
      <div style="background: white; border-radius: 0.75rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e5e7eb; overflow: hidden;">
        <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.95rem;">
          <thead>
            <tr style="background-color: #f9fafb; border-bottom: 1px solid #e5e7eb; color: #4b5563; font-weight: 600; text-transform: uppercase; font-size: 0.75rem; letter-spacing: 0.05em;">
              <th style="padding: 1rem 1.5rem;">ID</th>
              <th style="padding: 1rem 1.5rem;">Producto</th>
              <th style="padding: 1rem 1.5rem;">Cantidad</th>
              <th style="padding: 1rem 1.5rem;">Estado</th>
              <th style="padding: 1rem 1.5rem;">Costo</th>
              <th style="padding: 1rem 1.5rem; text-align: right;">Acciones</th>
            </tr>
          </thead>
          <tbody style="color: #374151;">
            @for (orden of ordenService.ordenes(); track orden.id) {
              <tr style="border-bottom: 1px solid #f3f4f6; transition: background-color 0.15s;">
                <td style="padding: 1rem 1.5rem; font-weight: 600; color: #6b7280;">
                  #{{ orden.id }}
                </td>
                <td style="padding: 1rem 1.5rem;">
                  <div style="font-weight: 600; color: #111827;">{{ orden.producto }}</div>
                  @if (orden.loteGenerado) {
                    <span style="font-size: 0.75rem; color: #9ca3af; display: block; margin-top: 0.125rem;">
                      Lote: {{ orden.loteGenerado }}
                    </span>
                  }
                  @if (orden.centroTrabajo) {
                    <span style="font-size: 0.75rem; color: #6b7280; display: block;">
                      🏭 {{ orden.centroTrabajo.nombre }}
                    </span>
                  }
                </td>
                <td style="padding: 1rem 1.5rem; font-weight: 500;">
                  {{ orden.cantidadPlanificada }} kg/uds
                </td>
                <td style="padding: 1rem 1.5rem;">
                  <span [style.background-color]="getEstadoBadgeBg(orden.estado)"
                        [style.color]="getEstadoBadgeColor(orden.estado)"
                        style="padding: 0.25rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.025em; display: inline-block;">
                    {{ orden.estado }}
                  </span>
                </td>
                <td style="padding: 1rem 1.5rem; font-weight: 600; color: #047857;">
                  {{ orden.totalCostoInsumos | currency:'USD':'symbol':'1.2-2' }}
                </td>
                <td style="padding: 1rem 1.5rem; text-align: right;">
                  <button 
                    (click)="editar(orden.id)"
                    style="background-color: #f59e0b; color: white; border: none; padding: 0.375rem 0.75rem; border-radius: 0.375rem; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; margin-right: 0.5rem;">
                    Editar
                  </button>
                  <button 
                    (click)="eliminar(orden.id)"
                    style="background: #fee2e2; color: #dc2626; border: 1px solid #fca5a5; padding: 0.375rem 0.75rem; border-radius: 0.375rem; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s;">
                    Eliminar
                  </button>
                  <!-- Agrega este botón justo antes del botón de Eliminar -->
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="6" style="padding: 3rem; text-align: center; color: #6b7280;">
                  <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">🍫</div>
                  <p style="font-size: 1.1rem; font-weight: 600; color: #374151; margin: 0;">
                    No hay órdenes de producción registradas
                  </p>
                  <p style="font-size: 0.875rem; margin-top: 0.375rem; color: #9ca3af;">
                    Crea una nueva orden planificada para iniciar la manufactura de cacao.
                  </p>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class OrdenListComponent implements OnInit {
  readonly ordenService = inject(OrdenProduccionService);
  private router = inject(Router);

  ngOnInit(): void {
    this.ordenService.listar();
  }

  editar(id: number) {
    this.router.navigate(['/produccion/ordenes/editar', id]);
  }

  eliminar(id: number): void {
    const confirmado = confirm(`¿Está seguro de eliminar la orden de producción #${id}? Esta acción no se puede deshacer.`);
    if (confirmado) {
      this.ordenService.eliminar(id).subscribe({
        error: (err) => console.error(`Error al eliminar la orden #${id}:`, err)
      });
    }
  }

  getEstadoBadgeBg(estado: EstadoOrdenProduccion | string): string {
    switch (estado) {
      case EstadoOrdenProduccion.PLANIFICADO:
      case 'PLANIFICADO':
        return '#dbeafe';
      case EstadoOrdenProduccion.EN_PROCESO:
      case 'EN_PROCESO':
        return '#fef3c7';
      case EstadoOrdenProduccion.COMPLETADO:
      case 'COMPLETADO':
        return '#d1fae5';
      case EstadoOrdenProduccion.CANCELADO:
      case 'CANCELADO':
        return '#fee2e2';
      default:
        return '#f3f4f6';
    }
  }

  getEstadoBadgeColor(estado: EstadoOrdenProduccion | string): string {
    switch (estado) {
      case EstadoOrdenProduccion.PLANIFICADO:
      case 'PLANIFICADO':
        return '#1d4ed8';
      case EstadoOrdenProduccion.EN_PROCESO:
      case 'EN_PROCESO':
        return '#b45309';
      case EstadoOrdenProduccion.COMPLETADO:
      case 'COMPLETADO':
        return '#047857';
      case EstadoOrdenProduccion.CANCELADO:
      case 'CANCELADO':
        return '#b91c1c';
      default:
        return '#374151';
    }
  }
}