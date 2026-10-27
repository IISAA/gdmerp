import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe, Location } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ProductoReporteService, StockConsolidadoDto } from '../services/producto-reporte.service';

@Component({
  selector: 'app-reporte-stock',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    MatTableModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './reporte-stock.component.html',
  styleUrls: ['./reporte-stock.component.css']
})
export class ReporteStockComponent implements OnInit {
  private readonly reporteService = inject(ProductoReporteService);
  private readonly location = inject(Location);

  readonly datosStock = signal<StockConsolidadoDto[]>([]);
  readonly textoBusqueda = signal<string>('');
  readonly isLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly displayedColumns: string[] = [
    'sku',
    'nombre',
    'categoria',
    'almacen',
    'stockFisico'
  ];

  readonly datosStockFiltrados = computed(() => {
    const termino = this.textoBusqueda().toLowerCase().trim();
    const datos = this.datosStock();
    if (!termino) return datos;

    return datos.filter(item =>
      (item.nombreProducto && item.nombreProducto.toLowerCase().includes(termino)) ||
      (item.nombre && item.nombre.toLowerCase().includes(termino)) ||
      (item.sku && item.sku.toLowerCase().includes(termino)) ||
      (item.categoria && item.categoria.toLowerCase().includes(termino)) ||
      (item.almacen && item.almacen.toLowerCase().includes(termino)) ||
      String(item.productoId || item.id || '').includes(termino)
    );
  });

  readonly totalProductos = computed(() => this.datosStockFiltrados().length);
  readonly totalStockFisico = computed(() =>
    this.datosStockFiltrados().reduce((total, item) => total + (item.stockFisico ?? item.stockTotal ?? 0), 0)
  );

  onBuscar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
  }

  limpiarBusqueda(): void {
    this.textoBusqueda.set('');
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  volver(): void {
    this.location.back();
  }

  cargarDatos(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.reporteService.obtenerStockConsolidado().subscribe({
      next: (datos) => {
        this.datosStock.set(datos || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error al cargar reporte de stock consolidado', err);
        this.error.set('No se pudo obtener el consolidado de existencias. Verifique la conexión con el servidor.');
        this.isLoading.set(false);
      }
    });
  }
}
