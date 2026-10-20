import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HasRoleDirective } from '../../../../core/directives/has-role.directive';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion, OrdenProduccionResponse } from '../../models/produccion.models';

@Component({
  selector: 'app-orden-list',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, MatButtonModule, MatIconModule, MatTooltipModule, HasRoleDirective],
  templateUrl: './orden-list.component.html'
})
export class OrdenListComponent implements OnInit {
  readonly ordenService = inject(OrdenProduccionService);
  private router = inject(Router);

  // 1. Estado principal (Data real del backend)
  protected ordenes = this.ordenService.ordenes;

  // 2. Estado de búsqueda
  protected textoBusqueda = signal<string>('');

  // 3. Estado derivado: La tabla SIEMPRE pintará esto
  protected ordenesFiltradas = computed(() => {
    const termino = this.textoBusqueda().toLowerCase().trim();
    if (!termino) {
      return this.ordenes(); // Si está vacío, muestra todo
    }

    return this.ordenes().filter(orden => {
      const productoMatch = orden.producto?.toLowerCase().includes(termino);
      const loteMatch = orden.loteGenerado?.toLowerCase().includes(termino);
      const estadoMatch = orden.estado?.toLowerCase().includes(termino);

      return Boolean(productoMatch || loteMatch || estadoMatch);
    });
  });

  ngOnInit(): void {
    this.cargarOrdenes();
  }

  cargarOrdenes(): void {
    this.ordenService.listar();
  }

  // Método para capturar el input del usuario
  onBuscar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
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