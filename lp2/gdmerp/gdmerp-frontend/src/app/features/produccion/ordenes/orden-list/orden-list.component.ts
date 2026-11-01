import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HasRoleDirective } from '../../../../shared/directives/has-role.directive';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion } from '../../models/produccion.models';

@Component({
  selector: 'app-orden-list',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    RouterLink,
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    HasRoleDirective
  ],
  templateUrl: './orden-list.component.html',
  styleUrls: ['./orden-list.component.css']
})
export class OrdenListComponent implements OnInit {
  readonly ordenService = inject(OrdenProduccionService);
  private readonly router = inject(Router);

  // 1. Estado de alternancia de vista (Cards vs Lista)
  readonly modoVista = signal<'cards' | 'lista'>('cards');

  // 2. Filtro reactivo por estado (activado desde las tarjetas KPI)
  readonly filtroEstado = signal<string | null>(null);

  // 3. Filtro reactivo por texto de búsqueda
  readonly textoBusqueda = signal<string>('');

  // 4. Indicador de carga
  readonly isLoading = this.ordenService.isLoading;

  // 5. Estado principal (Data reactiva de órdenes)
  protected ordenes = this.ordenService.ordenes;

  // 6. Lista reactiva filtrada para alimentar la tabla
  readonly ordenesFiltradas = computed(() => {
    const estado = this.filtroEstado();
    const termino = this.textoBusqueda().toLowerCase().trim();
    let lista = this.ordenes();

    if (estado) {
      lista = lista.filter(o => o.estado === estado);
    }

    if (termino) {
      lista = lista.filter(o =>
        (o.producto && o.producto.toLowerCase().includes(termino)) ||
        (o.loteGenerado && o.loteGenerado.toLowerCase().includes(termino)) ||
        (o.estado && o.estado.toLowerCase().includes(termino)) ||
        (o.centroTrabajo?.nombre && o.centroTrabajo.nombre.toLowerCase().includes(termino)) ||
        String(o.id).includes(termino)
      );
    }

    return lista;
  });

  // 5. Columnas para la tabla en modo lista
  readonly displayedColumns: string[] = [
    'id',
    'producto',
    'cantidadPlanificada',
    'horasTrabajadas',
    'estado',
    'costoTotal'
  ];

  // 6. Métricas reactivas (Dashboard KPIs)
  readonly totalPlanificadas = computed(() =>
    this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.PLANIFICADO).length
  );

  readonly totalEnProceso = computed(() =>
    this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.EN_PROCESO).length
  );

  readonly totalCompletadas = computed(() =>
    this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.COMPLETADO).length
  );

  readonly totalCanceladas = computed(() =>
    this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.CANCELADO).length
  );

  readonly totalOrdenes = computed(() => this.ordenes().length);

  ngOnInit(): void {
    this.cargarOrdenes();
  }

  cargarOrdenes(): void {
    this.ordenService.listar();
  }

  // Toggle de vista con limpieza de filtros
  alternarVista(): void {
    this.modoVista.set(this.modoVista() === 'cards' ? 'lista' : 'cards');
    this.filtroEstado.set(null);
    this.textoBusqueda.set('');
  }

  // Filtrado reactivo al hacer clic en una tarjeta KPI
  filtrarPorEstado(estado: string): void {
    this.filtroEstado.set(estado);
    this.modoVista.set('lista');
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
  }

  irADetalle(id: number): void {
    this.router.navigate(['/produccion/ordenes', id]);
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
