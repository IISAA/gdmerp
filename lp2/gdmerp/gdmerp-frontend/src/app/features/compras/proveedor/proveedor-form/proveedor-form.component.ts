import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ProveedorService } from '../services/proveedor.service';
import { RucConsultaResponse } from '../models/proveedor.model';

@Component({
  selector: 'app-proveedor-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatSnackBarModule, MatIconModule],
  templateUrl: './proveedor-form.component.html',
  styleUrl: './proveedor-form.component.css',
})
export class ProveedorForm {
  private readonly fb = inject(FormBuilder);
  private readonly proveedorService = inject(ProveedorService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly buscandoRuc = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly rucStatus = signal<{ estado: string; condicion: string } | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(/^[0-9]{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    email: ['', [Validators.email]],
    direccion: ['', [Validators.maxLength(200)]],
  });

  consultarRuc(): void {
    const ruc = this.form.controls.ruc.value.trim();
    if (!/^[0-9]{11}$/.test(ruc)) {
      this.error.set('Ingrese un RUC válido de 11 dígitos para consultar en SUNAT.');
      this.mostrarMensaje('Ingrese un RUC válido de 11 dígitos para consultar en SUNAT.', 'warning');
      return;
    }

    this.buscandoRuc.set(true);
    this.error.set(null);

    this.proveedorService.consultarRuc(ruc).subscribe({
      next: (data: RucConsultaResponse) => {
        this.buscandoRuc.set(false);
        if (data.razonSocial) {
          this.form.patchValue({
            razonSocial: data.razonSocial,
            direccion: data.direccion || '',
          });
          this.rucStatus.set({
            estado: data.estado || 'ACTIVO',
            condicion: data.condicion || 'HABIDO',
          });
          this.mostrarMensaje(`Datos obtenidos de SUNAT para ${data.razonSocial}`, 'success');
        } else {
          this.mostrarMensaje('No se obtuvieron datos fiscales para el RUC ingresado.', 'warning');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.buscandoRuc.set(false);
        const msg = (err.error as { message?: string } | null)?.message || 'No se pudo consultar el RUC en SUNAT / APIsPERU.';
        this.error.set(msg);
        this.mostrarMensaje(msg, 'error');
      },
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mostrarMensaje('Por favor complete los campos obligatorios del formulario.', 'warning');
      return;
    }

    this.loading.set(true);
    const val = this.form.getRawValue();

    this.proveedorService.crearProveedor(val).subscribe({
      next: () => {
        this.mostrarMensaje(`Proveedor "${val.razonSocial}" registrado exitosamente.`, 'success');
        this.router.navigate(['/compras/proveedores']);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message || 'Error al registrar el proveedor.';
        this.error.set(msg);
        this.mostrarMensaje(msg, 'error');
        this.loading.set(false);
      },
    });
  }

  protected mensajeValidacion(campo: 'ruc' | 'razonSocial' | 'email'): string {
    const control = this.form.controls[campo];
    if (!control.touched) return '';

    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('pattern')) return 'El RUC debe tener exactamente 11 dígitos numéricos.';
    if (control.hasError('email')) return 'Ingrese un formato de correo válido.';
    return '';
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
