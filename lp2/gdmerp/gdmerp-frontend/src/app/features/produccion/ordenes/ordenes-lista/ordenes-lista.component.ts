import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { HasRoleDirective } from '../../../../core/directives/has-role.directive';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion } from '../../models/produccion.models';

@Component({
  selector: 'app-ordenes-lista',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    RouterLink,
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    HasRoleDirective
  ],
  templateUrl: './ordenes-lista.component.html',
  styleUrls: ['./ordenes-lista.component.css']
})
export class OrdenesListaComponent implements OnInit {
  readonly ordenService = inject(OrdenProduccionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // 1. Estado principal (Data de órdenes)
  protected ordenes = this.ordenService.ordenes;
  readonly isLoading = this.ordenService.isLoading;

  // 2. Filtros reactivos mediante Signals
  readonly filtroEstado = signal<string | null>(null);
  readonly textoBusqueda = signal<string>('');

  // 3. Columnas de la tabla Angular Material
  readonly displayedColumns: string[] = [
    'id',
    'producto',
    'cantidadPlanificada',
    'estado',
    'totalCostoInsumos'
  ];

  // 4. Lista filtrada automáticamente (Pre-filtro por estado URL + Buscador)
  readonly ordenesFiltradas = computed(() => {
    const estadoFiltro = this.filtroEstado();
    const termino = this.textoBusqueda().toLowerCase().trim();

    return this.ordenes().filter(orden => {
      // 4.1 Filtro por Estado (Query Param de Drill-down)
      if (estadoFiltro && orden.estado !== estadoFiltro) {
        return false;
      }

      // 4.2 Filtro por Texto de Búsqueda
      if (termino) {
        const productoMatch = orden.producto?.toLowerCase().includes(termino);
        const loteMatch = orden.loteGenerado?.toLowerCase().includes(termino);
        const estadoMatch = orden.estado?.toLowerCase().includes(termino);
        const centroMatch = orden.centroTrabajo?.nombre?.toLowerCase().includes(termino);
        const idMatch = String(orden.id).includes(termino);

        if (!Boolean(productoMatch || loteMatch || estadoMatch || centroMatch || idMatch)) {
          return false;
        }
      }

      return true;
    });
  });

  ngOnInit(): void {
    // Escucha activa de los Query Parameters para pre-filtrar
    this.route.queryParamMap.subscribe(params => {
      const estadoParam = params.get('estado');
      this.filtroEstado.set(estadoParam ? estadoParam.toUpperCase() : null);
    });

    this.cargarOrdenes();
  }

  cargarOrdenes(): void {
    this.ordenService.listar();
  }

  onBuscar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
  }

  limpiarBusqueda(): void {
    this.textoBusqueda.set('');
  }

  limpiarFiltroEstado(): void {
    this.filtroEstado.set(null);
    this.router.navigate(['/produccion/ordenes/lista']);
  }

  irADetalle(id: number): void {
    this.router.navigate(['/produccion/ordenes', id]);
  }

  editar(id: number): void {
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
