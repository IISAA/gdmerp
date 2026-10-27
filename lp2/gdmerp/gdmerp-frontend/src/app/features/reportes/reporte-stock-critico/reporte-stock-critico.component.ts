import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe, Location } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ProductoReporteService, StockCriticoDto } from '../services/producto-reporte.service';

@Component({
  selector: 'app-reporte-stock-critico',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    MatTableModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './reporte-stock-critico.component.html',
  styleUrls: ['./reporte-stock-critico.component.css']
})
export class ReporteStockCriticoComponent implements OnInit {
  private readonly reporteService = inject(ProductoReporteService);
  private readonly location = inject(Location);

  readonly datosReporte = signal<StockCriticoDto[]>([]);
  readonly textoBusqueda = signal<string>('');
  readonly isLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly displayedColumns: string[] = [
    'sku',
    'nombre',
    'stockActual',
    'stockMinimo',
    'estado'
  ];

  readonly datosReporteFiltrados = computed(() => {
    const termino = this.textoBusqueda().toLowerCase().trim();
    const datos = this.datosReporte();
    if (!termino) return datos;

    return datos.filter(item =>
      (item.nombreProducto && item.nombreProducto.toLowerCase().includes(termino)) ||
      (item.nombre && item.nombre.toLowerCase().includes(termino)) ||
      (item.sku && item.sku.toLowerCase().includes(termino)) ||
      (item.estado && item.estado.toLowerCase().includes(termino)) ||
      String(item.productoId || item.id || '').includes(termino)
    );
  });

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

    this.reporteService.obtenerStockCritico().subscribe({
      next: (datos) => {
        this.datosReporte.set(datos || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error al cargar reporte de stock crítico', err);
        this.error.set('No se pudo cargar el reporte de stock crítico. Verifique la conexión con el servidor.');
        this.isLoading.set(false);
      }
    });
  }
}
