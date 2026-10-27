import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { OrdenReporteService, OrdenesPorEstadoDto } from '../services/orden-reporte.service';

@Component({
  selector: 'app-reporte-ordenes',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './reporte-ordenes.component.html',
  styleUrls: ['./reporte-ordenes.component.css']
})
export class ReporteOrdenesComponent implements OnInit {
  private readonly ordenReporteService = inject(OrdenReporteService);
  private readonly location = inject(Location);

  readonly datosEstados = signal<OrdenesPorEstadoDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly displayedColumns: string[] = ['estado', 'cantidadOrdenes'];

  readonly totalOrdenes = computed(() =>
    this.datosEstados().reduce((sum, item) => sum + (Number(item.cantidadOrdenes) || 0), 0)
  );

  ngOnInit(): void {
    this.cargarDatos();
  }

  volver(): void {
    this.location.back();
  }

  cargarDatos(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.ordenReporteService.obtenerResumenPorEstado().subscribe({
      next: (datos) => {
        this.datosEstados.set(datos || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error al cargar reporte de órdenes por estado', err);
        this.error.set('No se pudo cargar el resumen de órdenes por estado. Verifique la conexión con el servidor.');
        this.isLoading.set(false);
      }
    });
  }

  getEstadoBadgeBg(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'PLANIFICADO':
        return '#dbeafe';
      case 'EN_PROCESO':
        return '#fef3c7';
      case 'COMPLETADO':
        return '#d1fae5';
      case 'CANCELADO':
        return '#fee2e2';
      default:
        return '#f3f4f6';
    }
  }

  getEstadoBadgeColor(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'PLANIFICADO':
        return '#1d4ed8';
      case 'EN_PROCESO':
        return '#b45309';
      case 'COMPLETADO':
        return '#047857';
      case 'CANCELADO':
        return '#dc2626';
      default:
        return '#4b5563';
    }
  }

  getEstadoIcon(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'PLANIFICADO':
        return 'event_note';
      case 'EN_PROCESO':
        return 'autorenew';
      case 'COMPLETADO':
        return 'check_circle';
      case 'CANCELADO':
        return 'cancel';
      default:
        return 'help_outline';
    }
  }
}
