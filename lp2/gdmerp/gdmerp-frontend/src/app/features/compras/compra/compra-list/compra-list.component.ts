import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { CompraService } from '../services/compra.service';
import { Compra, CompraReporte, PagoCompra } from '../models/compra.model';

@Component({
  selector: 'app-compra-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule, CurrencyPipe, DatePipe, MatSnackBarModule, MatIconModule],
  templateUrl: './compra-list.component.html',
  styleUrl: './compra-list.component.css',
})
export class CompraList implements OnInit {
  private readonly compraService = inject(CompraService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly compras = signal<Compra[]>([]);
  protected readonly resumen = signal<CompraReporte | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  // Filtros interactivos
  protected readonly estadoFiltro = signal<string>('TODAS');
  protected readonly desdeFiltro = signal<string>('');
  protected readonly hastaFiltro = signal<string>('');
  protected readonly busquedaTexto = signal<string>('');

  // Modales
  protected readonly compraSeleccionada = signal<Compra | null>(null);
  protected readonly modalAmortizar = signal(false);
  protected readonly modalDetalle = signal(false);
  protected readonly historialPagos = signal<PagoCompra[]>([]);
  protected readonly tabDetalle = signal<'INSUMOS' | 'PAGOS'>('INSUMOS');

  protected readonly formAmortizar = this.fb.nonNullable.group({
    montoPago: [0, [Validators.required, Validators.min(0.01)]],
    metodoPago: ['TRANSFERENCIA', [Validators.required]],
    referencia: [''],
  });

  // KPIs Reactivos
  protected readonly totalComprasCalc = computed(() => this.compras().length);

  protected readonly totalFacturadoCalc = computed(() =>
    this.compras().reduce(
      (acc: number, c: Compra) => acc + (c.estado !== 'ANULADA' ? Number(c.total || 0) : 0),
      0
    )
  );

  protected readonly saldoPendienteCalc = computed(() =>
    this.compras().reduce(
      (acc: number, c: Compra) => acc + (c.estado !== 'ANULADA' ? Number(c.saldoPendiente || 0) : 0),
      0
    )
  );

  protected readonly totalPagadoCalc = computed(() =>
    Math.max(0, this.totalFacturadoCalc() - this.saldoPendienteCalc())
  );

  protected readonly comprasFiltradas = computed(() => {
    const q = this.busquedaTexto().trim().toLowerCase();
    if (!q) return this.compras();
    return this.compras().filter((c: Compra) =>
      c.numeroComprobante.toLowerCase().includes(q) ||
      (c.serieComprobante && c.serieComprobante.toLowerCase().includes(q)) ||
      (c.proveedor?.razonSocial && c.proveedor.razonSocial.toLowerCase().includes(q)) ||
      (c.proveedor?.ruc && c.proveedor.ruc.includes(q))
    );
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    const est = this.estadoFiltro();
    const desde = this.desdeFiltro() || undefined;
    const hasta = this.hastaFiltro() || undefined;

    this.compraService.listarCompras(est, desde, hasta).subscribe({
      next: (data: Compra[]) => {
        this.compras.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el historial de compras.');
        this.loading.set(false);
      },
    });

    this.compraService.reporte(est, desde, hasta).subscribe({
      next: (rep: CompraReporte) => this.resumen.set(rep),
      error: () => {},
    });
  }

  filtrarPorEstado(estado: string): void {
    this.estadoFiltro.set(estado);
    this.cargarDatos();
  }

  limpiarFiltros(): void {
    this.estadoFiltro.set('TODAS');
    this.desdeFiltro.set('');
    this.hastaFiltro.set('');
    this.busquedaTexto.set('');
    this.cargarDatos();
  }

  abrirAmortizar(compra: Compra): void {
    this.compraSeleccionada.set(compra);
    this.formAmortizar.patchValue({
      montoPago: compra.saldoPendiente,
      referencia: '',
    });
    this.modalAmortizar.set(true);
  }

  confirmarAmortizar(): void {
    const c = this.compraSeleccionada();
    if (!c || this.formAmortizar.invalid) return;

    this.loading.set(true);
    this.error.set(null);
    const val = this.formAmortizar.getRawValue();

    this.compraService.amortizarPago(c.id, val).subscribe({
      next: () => {
        const msg = `✅ Pago de S/ ${val.montoPago.toFixed(2)} amortizado con éxito en la factura ${c.numeroComprobante}.`;
        this.successMsg.set(msg);
        this.snackBar.open(msg, 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar'],
        });
        this.modalAmortizar.set(false);
        this.cargarDatos();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message || 'Error al amortizar el pago.';
        this.error.set(msg);
        this.loading.set(false);
        this.snackBar.open(`❌ ${msg}`, 'Entendido', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  anular(compra: Compra): void {
    if (!confirm(`¿Desea anular la compra ${compra.numeroComprobante}? Se anularán los saldos pendientes y pagos asociados.`)) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.compraService.anularCompra(compra.id).subscribe({
      next: () => {
        const msg = `✅ Compra ${compra.numeroComprobante} anulada correctamente.`;
        this.successMsg.set(msg);
        this.snackBar.open(msg, 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['info-snackbar'],
        });
        this.cargarDatos();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message || 'Error al anular la compra.';
        this.error.set(msg);
        this.loading.set(false);
        this.snackBar.open(`❌ ${msg}`, 'Entendido', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  verDetalle(compra: Compra): void {
    this.tabDetalle.set('INSUMOS');
    this.compraService.obtenerCompra(compra.id).subscribe({
      next: (det: Compra) => {
        this.compraSeleccionada.set(det);
        this.modalDetalle.set(true);
        this.cargarPagos(det.id);
      },
      error: () => {
        this.compraSeleccionada.set(compra);
        this.modalDetalle.set(true);
        this.cargarPagos(compra.id);
      },
    });
  }

  private cargarPagos(compraId: number): void {
    this.compraService.listarPagos(compraId).subscribe({
      next: (pagos: PagoCompra[]) => this.historialPagos.set(pagos || []),
      error: () => this.historialPagos.set([]),
    });
  }
}
