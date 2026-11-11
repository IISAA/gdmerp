import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CuentaCobrarService } from '../services/cuenta-cobrar.service';
import { CrearCreditoRequest, CuentaPorCobrarDetalle } from '../models/cuenta-cobrar.model';

@Component({
  selector: 'app-cuenta-cobrar-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cuenta-cobrar-form.component.html',
  styleUrls: ['./cuenta-cobrar-form.component.css'],
})
export class CuentaCobrarForm implements OnInit {
  private readonly cuentaService = inject(CuentaCobrarService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly guardando = signal(false);
  protected readonly mensajeError = signal<string | null>(null);
  protected readonly modoEdicion = signal(false);
  protected readonly creditoId = signal<number | null>(null);
  protected readonly detalle = signal<CuentaPorCobrarDetalle | null>(null);

  // Campos del Formulario
  protected readonly codigo = signal<string>('');
  protected readonly clienteNombre = signal<string>('');
  protected readonly clienteDocumento = signal<string>('');
  protected readonly montoTotal = signal<number>(0);
  protected readonly fechaEmision = signal<string>('');
  protected readonly fechaVencimiento = signal<string>('');
  protected readonly nota = signal<string>('');

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.modoEdicion.set(true);
      this.creditoId.set(Number(idParam));
      this.cargarDetalle(Number(idParam));
    } else {
      this.initNuevo();
    }
  }

  private initNuevo(): void {
    const today = new Date();
    const nextMonth = new Date();
    nextMonth.setDate(today.getDate() + 30);

    this.fechaEmision.set(today.toISOString().split('T')[0]);
    this.fechaVencimiento.set(nextMonth.toISOString().split('T')[0]);
  }

  private cargarDetalle(id: number): void {
    this.loading.set(true);
    this.cuentaService.obtenerPorId(id).subscribe({
      next: (data) => {
        this.detalle.set(data);
        this.codigo.set(data.codigo);
        this.clienteNombre.set(data.clienteNombre);
        this.clienteDocumento.set(data.clienteDocumento || '');
        this.montoTotal.set(data.montoTotal);
        this.fechaEmision.set(data.fechaEmision);
        this.fechaVencimiento.set(data.fechaVencimiento);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.mensajeError.set(err.error?.message || 'Error al cargar el crédito.');
      },
    });
  }

  guardar(): void {
    const nombre = this.clienteNombre().trim();
    const monto = Number(this.montoTotal());

    if (!nombre) {
      this.mensajeError.set('El nombre o razón social del cliente es obligatorio.');
      return;
    }

    if (isNaN(monto) || monto <= 0) {
      this.mensajeError.set('El monto total del crédito debe ser un número mayor a cero.');
      return;
    }

    this.guardando.set(true);
    this.mensajeError.set(null);

    const req: CrearCreditoRequest = {
      codigo: this.codigo().trim() || undefined,
      clienteNombre: nombre,
      clienteDocumento: this.clienteDocumento().trim() || undefined,
      montoTotal: monto,
      fechaEmision: this.fechaEmision() || undefined,
      fechaVencimiento: this.fechaVencimiento() || undefined,
      nota: this.nota().trim() || undefined,
    };

    this.cuentaService.registrarCredito(req).subscribe({
      next: () => {
        this.guardando.set(false);
        this.router.navigate(['/finanzas/creditos']);
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeError.set(err.error?.message || 'Error al guardar la cuenta por cobrar.');
      },
    });
  }
}
