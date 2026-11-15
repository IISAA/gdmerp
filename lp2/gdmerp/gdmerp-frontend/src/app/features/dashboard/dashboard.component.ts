import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { OrdenProduccionService } from '../produccion/services/orden-produccion.service';
import { EstadoOrdenProduccion } from '../produccion/models/produccion.models';
import { ProductoReporteService, StockConsolidadoDto, StockCriticoDto } from '../reportes/services/producto-reporte.service';
import { VentaService } from '../ventas/services/venta.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, CurrencyPipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private readonly ordenService = inject(OrdenProduccionService);
  private readonly productoReporteService = inject(ProductoReporteService);
  private readonly ventaService = inject(VentaService);

  readonly ordenes = this.ordenService.ordenes;
  readonly stockConsolidado = signal<StockConsolidadoDto[]>([]);
  readonly stockCritico = signal<StockCriticoDto[]>([]);
  readonly totalRecaudadoVentas = signal<number>(0);
  readonly cargandoMetricas = signal<boolean>(true);

  // KPIs Calculados basados en datos reales
  readonly totalOrdenesRegistradas = computed(() => this.ordenes().length);

  readonly ordenesActivas = computed(() => {
    return this.ordenes().filter(
      o => o.estado === EstadoOrdenProduccion.EN_PROCESO || o.estado === EstadoOrdenProduccion.PLANIFICADO
    ).length;
  });

  readonly totalPlanificadas = computed(() => {
    return this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.PLANIFICADO).length;
  });

  readonly totalEnProceso = computed(() => {
    return this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.EN_PROCESO).length;
  });

  readonly totalCompletadas = computed(() => {
    return this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.COMPLETADO).length;
  });

  readonly totalCanceladas = computed(() => {
    return this.ordenes().filter(o => o.estado === EstadoOrdenProduccion.CANCELADO).length;
  });

  readonly tasaCumplimiento = computed(() => {
    const total = this.totalOrdenesRegistradas();
    if (total === 0) return 100;
    const comp = this.totalCompletadas();
    return Math.round((comp / total) * 100);
  });

  readonly totalItemsInventario = computed(() => {
    return this.stockConsolidado().reduce(
      (sum, item) => sum + (Number(item.stockTotal || item.stockFisico || 0)), 
      0
    );
  });

  readonly cantidadAlertasCriticas = computed(() => this.stockCritico().length);

  ngOnInit(): void {
    this.cargarDatosReales();
  }

  cargarDatosReales(): void {
    this.cargandoMetricas.set(true);

    try {
      this.ordenService.listar();
    } catch (e) {
      console.warn('Error al listar órdenes de producción:', e);
    }

    this.productoReporteService.obtenerStockConsolidado().subscribe({
      next: (stock) => {
        this.stockConsolidado.set(stock || []);
      },
      error: () => {},
    });

    this.productoReporteService.obtenerStockCritico().subscribe({
      next: (criticos) => {
        this.stockCritico.set(criticos || []);
      },
      error: () => {},
    });

    this.ventaService.reporte().subscribe({
      next: (rep) => {
        const total = rep.totalRecaudado ?? rep.montoTotal ?? 0;
        this.totalRecaudadoVentas.set(total);
        this.cargandoMetricas.set(false);
      },
      error: () => {
        this.cargandoMetricas.set(false);
      },
    });
  }
}
