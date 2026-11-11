import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { CuentaCobrarService } from '../services/cuenta-cobrar.service';
import {
  CuentaPorCobrar,
  CuentaPorCobrarDetalle,
  CrearCreditoRequest,
  RegistrarAbonoRequest,
  EstadoCredito,
  MetodoPago,
} from '../models/cuenta-cobrar.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-cuenta-cobrar-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cuenta-cobrar-list.component.html',
  styleUrls: ['./cuenta-cobrar-list.component.css'],
})
export class CuentaCobrarList implements OnInit {
  protected readonly cuentaService = inject(CuentaCobrarService);
  protected readonly auth = inject(AuthService);

  // Lista principal y estado
  protected readonly creditos = signal<CuentaPorCobrar[]>([]);
  protected readonly loading = signal(false);
  protected readonly mensajeExito = signal<string | null>(null);
  protected readonly mensajeError = signal<string | null>(null);

  // Filtros
  protected readonly filtroEstado = signal<EstadoCredito | ''>('');
  protected readonly busqueda = signal<string>('');

  // Modales
  protected readonly mostrarModalAbono = signal(false);
  protected readonly mostrarModalDetalle = signal(false);
  protected readonly mostrarModalNuevo = signal(false);

  // Objeto seleccionado para operar
  protected readonly creditoSeleccionado = signal<CuentaPorCobrar | null>(null);
  protected readonly creditoDetalle = signal<CuentaPorCobrarDetalle | null>(null);
  protected readonly detalleLoading = signal(false);

  // Formulario de Abono / Cobro
  protected readonly abonoMonto = signal<number>(0);
  protected readonly abonoMetodo = signal<MetodoPago>('EFECTIVO');
  protected readonly abonoNota = signal<string>('');
  protected readonly abonoLoading = signal(false);
  protected readonly errorAbono = signal<string | null>(null);

  // Formulario de Nuevo Crédito
  protected readonly nuevoCodigo = signal<string>('');
  protected readonly nuevoClienteNombre = signal<string>('');
  protected readonly nuevoClienteDocumento = signal<string>('');
  protected readonly nuevoMontoTotal = signal<number>(0);
  protected readonly nuevoFechaEmision = signal<string>('');
  protected readonly nuevoFechaVencimiento = signal<string>('');
  protected readonly nuevoNota = signal<string>('');
  protected readonly nuevoLoading = signal(false);
  protected readonly errorNuevo = signal<string | null>(null);

  // Métricas Computadas
  protected readonly creditosFiltrados = computed(() => {
    const q = this.busqueda().toLowerCase().trim();
    const est = this.filtroEstado();
    return this.creditos().filter((c) => {
      const matchEstado = !est || c.estado === est;
      const matchTexto =
        !q ||
        c.codigo.toLowerCase().includes(q) ||
        c.clienteNombre.toLowerCase().includes(q) ||
        (c.clienteDocumento && c.clienteDocumento.toLowerCase().includes(q));
      return matchEstado && matchTexto;
    });
  });

  protected readonly totalCreditosMonto = computed(() =>
    this.creditos().reduce((acc, c) => acc + (c.montoTotal || 0), 0)
  );

  protected readonly totalCobradoMonto = computed(() =>
    this.creditos().reduce((acc, c) => acc + (c.montoPagado || 0), 0)
  );

  protected readonly totalPendienteMonto = computed(() =>
    this.creditos().reduce((acc, c) => acc + (c.saldoPendiente || 0), 0)
  );

  protected readonly tasaRecuperacion = computed(() => {
    const tot = this.totalCreditosMonto();
    if (tot <= 0) return 0;
    return Math.round((this.totalCobradoMonto() / tot) * 100);
  });

  ngOnInit(): void {
    this.cargarCreditos();
  }

  cargarCreditos(): void {
    this.loading.set(true);
    this.mensajeError.set(null);

    const est = this.filtroEstado() ? (this.filtroEstado() as EstadoCredito) : undefined;
    this.cuentaService.listar(est).subscribe({
      next: (data) => {
        this.creditos.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.mensajeError.set(
          err.error?.message || 'Error al cargar las cuentas por cobrar y créditos.'
        );
      },
    });
  }

  setFiltroEstado(estado: EstadoCredito | ''): void {
    this.filtroEstado.set(estado);
    this.cargarCreditos();
  }

  // --- Modal de Cobro / Abono ---
  abrirModalAbono(credito: CuentaPorCobrar): void {
    this.creditoSeleccionado.set(credito);
    this.abonoMonto.set(credito.saldoPendiente);
    this.abonoMetodo.set('EFECTIVO');
    this.abonoNota.set('');
    this.errorAbono.set(null);
    this.mostrarModalAbono.set(true);
  }

  cerrarModalAbono(): void {
    this.mostrarModalAbono.set(false);
    this.creditoSeleccionado.set(null);
    this.errorAbono.set(null);
  }

  setMontoAbonoParcial(porcentaje: number): void {
    const cred = this.creditoSeleccionado();
    if (!cred) return;
    const val = Number(((cred.saldoPendiente * porcentaje) / 100).toFixed(2));
    this.abonoMonto.set(val);
  }

  confirmarAbono(): void {
    const cred = this.creditoSeleccionado();
    if (!cred) return;

    const monto = Number(this.abonoMonto());
    if (isNaN(monto) || monto <= 0) {
      this.errorAbono.set('El monto a abonar debe ser un valor positivo mayor a cero.');
      return;
    }

    if (monto > cred.saldoPendiente) {
      this.errorAbono.set(
        `El monto a pagar (S/ ${monto.toFixed(2)}) no puede exceder el saldo pendiente (S/ ${cred.saldoPendiente.toFixed(2)}).`
      );
      return;
    }

    this.abonoLoading.set(true);
    this.errorAbono.set(null);

    const req: RegistrarAbonoRequest = {
      monto,
      metodoPago: this.abonoMetodo(),
      nota: this.abonoNota().trim() || undefined,
    };

    this.cuentaService.registrarAbono(cred.id, req).subscribe({
      next: (detalleActualizado) => {
        this.abonoLoading.set(false);
        this.cerrarModalAbono();
        this.mensajeExito.set(
          `¡Abono de S/ ${monto.toFixed(2)} registrado y sumado a su turno de caja activo! Crédito ${detalleActualizado.codigo} actualizado a ${detalleActualizado.estado}.`
        );
        this.cargarCreditos();
      },
      error: (err: HttpErrorResponse) => {
        this.abonoLoading.set(false);
        this.errorAbono.set(err.error?.message || 'Error al registrar el cobro/abono.');
      },
    });
  }

  // --- Modal de Detalle e Historial ---
  verDetalle(credito: CuentaPorCobrar): void {
    this.creditoSeleccionado.set(credito);
    this.mostrarModalDetalle.set(true);
    this.detalleLoading.set(true);

    this.cuentaService.obtenerPorId(credito.id).subscribe({
      next: (detalle) => {
        this.creditoDetalle.set(detalle);
        this.detalleLoading.set(false);
      },
      error: () => {
        this.detalleLoading.set(false);
      },
    });
  }

  cerrarModalDetalle(): void {
    this.mostrarModalDetalle.set(false);
    this.creditoSeleccionado.set(null);
    this.creditoDetalle.set(null);
  }

  // --- Modal de Nuevo Crédito ---
  abrirModalNuevo(): void {
    const today = new Date();
    const nextMonth = new Date();
    nextMonth.setDate(today.getDate() + 30);

    const fmtToday = today.toISOString().split('T')[0];
    const fmtNext = nextMonth.toISOString().split('T')[0];

    const count = this.creditos().length + 1;
    this.nuevoCodigo.set(`CRE-${count.toString().padStart(3, '0')}`);
    this.nuevoClienteNombre.set('');
    this.nuevoClienteDocumento.set('');
    this.nuevoMontoTotal.set(0);
    this.nuevoFechaEmision.set(fmtToday);
    this.nuevoFechaVencimiento.set(fmtNext);
    this.nuevoNota.set('');
    this.errorNuevo.set(null);
    this.mostrarModalNuevo.set(true);
  }

  cerrarModalNuevo(): void {
    this.mostrarModalNuevo.set(false);
    this.errorNuevo.set(null);
  }

  confirmarNuevoCredito(): void {
    const nombre = this.nuevoClienteNombre().trim();
    const monto = Number(this.nuevoMontoTotal());

    if (!nombre) {
      this.errorNuevo.set('El nombre o razón social del cliente es obligatorio.');
      return;
    }

    if (isNaN(monto) || monto <= 0) {
      this.errorNuevo.set('El monto total del crédito debe ser un número mayor a cero.');
      return;
    }

    this.nuevoLoading.set(true);
    this.errorNuevo.set(null);

    const req: CrearCreditoRequest = {
      codigo: this.nuevoCodigo().trim() || undefined,
      clienteNombre: nombre,
      clienteDocumento: this.nuevoClienteDocumento().trim() || undefined,
      montoTotal: monto,
      fechaEmision: this.nuevoFechaEmision() || undefined,
      fechaVencimiento: this.nuevoFechaVencimiento() || undefined,
      nota: this.nuevoNota().trim() || undefined,
    };

    this.cuentaService.registrarCredito(req).subscribe({
      next: (nuevo) => {
        this.nuevoLoading.set(false);
        this.cerrarModalNuevo();
        this.mensajeExito.set(
          `¡Crédito comercial ${nuevo.codigo} para ${nuevo.clienteNombre} registrado exitosamente por S/ ${nuevo.montoTotal.toFixed(2)}!`
        );
        this.cargarCreditos();
      },
      error: (err: HttpErrorResponse) => {
        this.nuevoLoading.set(false);
        this.errorNuevo.set(err.error?.message || 'Error al crear la cuenta por cobrar.');
      },
    });
  }

  // --- Helpers de Estilo y Badges ---
  getEstadoBadgeClass(estado: EstadoCredito): string {
    switch (estado) {
      case 'PENDIENTE':
        return 'badge-warning';
      case 'PAGADO_PARCIAL':
        return 'badge-info';
      case 'PAGADO_TOTAL':
        return 'badge-success';
      default:
        return 'badge-secondary';
    }
  }

  getEstadoLabel(estado: EstadoCredito): string {
    switch (estado) {
      case 'PENDIENTE':
        return 'Pendiente';
      case 'PAGADO_PARCIAL':
        return 'Pago Parcial';
      case 'PAGADO_TOTAL':
        return 'Pagado Total';
      default:
        return estado;
    }
  }

  esVencido(fechaVencimiento: string, estado: EstadoCredito): boolean {
    if (estado === 'PAGADO_TOTAL') return false;
    const due = new Date(fechaVencimiento);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return due < today;
  }
}

