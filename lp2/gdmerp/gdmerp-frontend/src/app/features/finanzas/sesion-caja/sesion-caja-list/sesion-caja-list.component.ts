import { Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { SesionCajaService } from '../services/sesion-caja.service';
import { CierreCajaReporte, SesionCaja, SesionCajaDetalle } from '../models/sesion-caja.model';

@Component({
  selector: 'app-sesion-caja-list',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './sesion-caja-list.component.html',
  styleUrls: ['./sesion-caja-list.component.css'],
})
export class SesionCajaList implements OnInit {
  private readonly sesionService = inject(SesionCajaService);
  private readonly router = inject(Router);

  protected readonly sesiones = signal<SesionCaja[]>([]);
  protected readonly sesionActiva = signal<SesionCaja | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  // Modal de Detalle / Arqueo
  protected readonly sesionSeleccionada = signal<SesionCajaDetalle | null>(null);
  protected readonly cargandoDetalle = signal(false);
  protected readonly exportando = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.sesionService.listar().subscribe({
      next: (data) => {
        this.sesiones.set(data);
        const activa = data.find((s) => s.estado === 'ABIERTA') ?? null;
        this.sesionActiva.set(activa);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el historial de sesiones de caja.');
        this.loading.set(false);
      },
    });
  }

  verTurnoActivo(): void {
    const activa = this.sesionActiva();
    if (activa) {
      this.router.navigate(['/finanzas/sesiones', activa.id]);
    } else {
      this.router.navigate(['/finanzas/sesiones/nueva']);
    }
  }

  abrirNuevoTurno(): void {
    this.router.navigate(['/finanzas/sesiones/nueva']);
  }

  verDetalle(s: SesionCaja): void {
    this.cargandoDetalle.set(true);
    this.sesionService.obtenerDetalle(s.id).subscribe({
      next: (det) => {
        this.sesionSeleccionada.set(det);
        this.cargandoDetalle.set(false);
      },
      error: () => {
        this.error.set('Error al cargar detalle del turno.');
        this.cargandoDetalle.set(false);
      },
    });
  }

  cerrarModal(): void {
    this.sesionSeleccionada.set(null);
  }

  exportar(sesionId: number, formato: 'PDF' | 'EXCEL'): void {
    this.exportando.set(sesionId);
    this.sesionService.exportar(sesionId, formato).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        const extension = formato === 'PDF' ? 'pdf' : 'xlsx';
        a.href = url;
        a.download = `cierre-caja-sesion-${sesionId}.${extension}`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportando.set(null);
      },
      error: () => {
        this.error.set(`No se pudo descargar el reporte en formato ${formato}.`);
        this.exportando.set(null);
      },
    });
  }
}

