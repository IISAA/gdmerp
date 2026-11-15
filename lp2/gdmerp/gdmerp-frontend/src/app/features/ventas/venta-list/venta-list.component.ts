import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { VentaService } from '../services/venta.service';
import { Venta, VentaReporte } from '../models/venta.model';

@Component({
  selector: 'app-venta-list',
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe, MatSnackBarModule, MatIconModule],
  standalone: true,
  templateUrl: './venta-list.component.html',
  styleUrl: './venta-list.component.css',
})
export class VentaListComponent implements OnInit {
  private readonly ventaService = inject(VentaService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly ventas = signal<Venta[]>([]);
  protected readonly resumen = signal<VentaReporte | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  protected readonly ventaSeleccionada = signal<Venta | null>(null);
  protected readonly modalTicket = signal(false);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.ventaService.buscar().subscribe({
      next: (data) => {
        this.ventas.set(data);
        this.loading.set(false);
      },
      error: () => {
        const errorMsg = 'No se pudo cargar el historial de ventas.';
        this.error.set(errorMsg);
        this.snackBar.open(errorMsg, 'Cerrar', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
        this.loading.set(false);
      },
    });

    this.ventaService.reporte().subscribe({
      next: (rep) => this.resumen.set(rep),
      error: () => {},
    });
  }

  verTicket(v: Venta): void {
    this.ventaService.obtener(v.id).subscribe({
      next: (ventaCompleta) => {
        this.ventaSeleccionada.set(ventaCompleta);
        this.modalTicket.set(true);
      },
      error: () => {
        this.ventaSeleccionada.set(v);
        this.modalTicket.set(true);
      },
    });
  }

  imprimirTicket(): void {
    window.print();
  }

  anularVenta(id: number): void {
    if (!confirm(`¿Está seguro de anular la venta #${id}? Esta acción repondrá automáticamente el stock de los productos.`)) {
      return;
    }

    this.loading.set(true);
    this.ventaService.anular(id).subscribe({
      next: () => {
        const msg = `Venta #${id} anulada con éxito y stock repuesto en catálogo.`;
        this.successMsg.set(msg);
        this.snackBar.open(msg, 'Cerrar', {
          duration: 3500,
          panelClass: ['success-snackbar'],
        });
        this.cargarDatos();
      },
      error: (err) => {
        const errMsg = err?.error?.message || 'Error al anular la venta.';
        this.error.set(errMsg);
        this.snackBar.open(errMsg, 'Cerrar', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
        this.loading.set(false);
      },
    });
  }
}

export { VentaListComponent as VentaList };
