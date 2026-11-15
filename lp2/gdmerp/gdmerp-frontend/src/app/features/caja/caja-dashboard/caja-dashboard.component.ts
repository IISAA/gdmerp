import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { CajaService } from '../services/caja.service';
import { SesionCaja, CajaReporte } from '../models/caja.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-caja-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, CurrencyPipe, DatePipe, MatSnackBarModule, MatIconModule],
  templateUrl: './caja-dashboard.component.html',
  styleUrl: './caja-dashboard.component.css',
})
export class CajaDashboard implements OnInit {
  private readonly cajaService = inject(CajaService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly authService = inject(AuthService);

  protected readonly sesionActiva = signal<SesionCaja | null>(null);
  protected readonly historial = signal<SesionCaja[]>([]);
  protected readonly resumen = signal<CajaReporte | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);

  // Modales
  protected readonly modalMovimiento = signal(false);
  protected readonly modalApertura = signal(false);
  protected readonly modalCierre = signal(false);

  // Formularios Reactivos sin datos hardcodeados
  protected readonly formMovimiento = this.fb.nonNullable.group({
    tipo: ['INGRESO' as 'INGRESO' | 'EGRESO', [Validators.required]],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    concepto: ['', [Validators.required, Validators.maxLength(100)]],
    metodoPago: ['EFECTIVO' as 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA', [Validators.required]],
    referencia: [''],
  });

  protected readonly formApertura = this.fb.nonNullable.group({
    cajeroNombre: ['', [Validators.required]],
    montoApertura: [0, [Validators.required, Validators.min(0)]],
    observaciones: [''],
  });

  protected readonly formCierre = this.fb.nonNullable.group({
    saldoReal: [0, [Validators.required, Validators.min(0)]],
    observaciones: [''],
  });

  ngOnInit(): void {
    const usuario = this.authService.usuario();
    if (usuario?.nombre) {
      this.formApertura.patchValue({
        cajeroNombre: usuario.nombre,
      });
    }
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.cajaService.obtenerSesionActiva().subscribe({
      next: (sesion) => {
        this.sesionActiva.set(sesion);
        if (sesion) {
          this.formCierre.patchValue({ saldoReal: sesion.saldoTeorico });
        }
      },
      error: () => {
        this.sesionActiva.set(null);
      },
    });

    this.cajaService.listarHistorial().subscribe({
      next: (hist) => this.historial.set(hist),
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar historial de caja:', err);
        this.error.set('No se pudo cargar el historial de sesiones de caja.');
        this.mostrarMensaje('No se pudo cargar el historial de sesiones de caja.', 'error');
      },
    });

    this.cajaService.obtenerResumen().subscribe({
      next: (rep) => {
        this.resumen.set(rep);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  registrarMovimiento(): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    if (this.formMovimiento.invalid) {
      this.formMovimiento.markAllAsTouched();
      this.mostrarMensaje('Por favor complete los datos del movimiento.', 'warning');
      return;
    }

    this.loading.set(true);
    const val = this.formMovimiento.getRawValue();

    this.cajaService.registrarMovimiento(sesion.id, val).subscribe({
      next: () => {
        this.mostrarMensaje(`Movimiento de ${val.tipo} por S/ ${val.monto.toFixed(2)} registrado correctamente.`, 'success');
        this.modalMovimiento.set(false);
        this.formMovimiento.reset({
          tipo: 'INGRESO',
          monto: 0,
          concepto: '',
          metodoPago: 'EFECTIVO',
          referencia: '',
        });
        this.cargarDatos();
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err?.error?.message || 'Error al registrar el movimiento.';
        this.error.set(errorMsg);
        this.mostrarMensaje(errorMsg, 'error');
        this.loading.set(false);
      },
    });
  }

  abrirTurno(): void {
    if (this.formApertura.invalid) {
      this.formApertura.markAllAsTouched();
      this.mostrarMensaje('Por favor complete los datos obligatorios para abrir la caja.', 'warning');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const val = this.formApertura.getRawValue();
    const usuarioId = this.authService.usuarioId();
    const roles = this.authService.roles() || [];
    const rol = roles.includes('VENDEDOR') ? 'VENDEDOR, CAJERO' : (roles[0] || 'CAJERO');

    this.cajaService.abrirCaja({
      ...val,
      usuarioId,
      cajeroRol: rol,
    }).subscribe({
      next: () => {
        this.mostrarMensaje('Nuevo turno de caja aperturado con éxito.', 'success');
        this.modalApertura.set(false);
        this.cargarDatos();
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err?.error?.message || err?.message || 'Error al aperturar caja.';
        this.error.set(errorMsg);
        this.mostrarMensaje(errorMsg, 'error');
        this.loading.set(false);
      },
    });
  }

  cerrarTurno(): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    this.loading.set(true);
    this.cajaService.cerrarCaja(sesion.id, this.formCierre.getRawValue()).subscribe({
      next: () => {
        this.mostrarMensaje('Turno de caja cerrado y arqueo asentado con éxito.', 'success');
        this.modalCierre.set(false);
        this.cargarDatos();
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err?.error?.message || 'Error al cerrar caja.';
        this.error.set(errorMsg);
        this.mostrarMensaje(errorMsg, 'error');
        this.loading.set(false);
      },
    });
  }

  private mostrarMensaje(mensaje: string, tipo: 'success' | 'error' | 'warning'): void {
    const panelClass = tipo === 'success' ? ['snackbar-success'] : tipo === 'error' ? ['snackbar-error'] : ['snackbar-warning'];
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass
    });
  }
}
