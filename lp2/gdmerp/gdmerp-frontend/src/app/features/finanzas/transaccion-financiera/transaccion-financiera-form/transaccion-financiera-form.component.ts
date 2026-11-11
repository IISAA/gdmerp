import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { CategoriaFinancieraService } from '../../categoria-financiera/services/categoria-financiera.service';
import {
  CategoriaFinanciera,
  EstadoFinanciero,
  TipoFinanciero,
} from '../../categoria-financiera/models/categoria-financiera.model';
import { TransaccionFinancieraService } from '../services/transaccion-financiera.service';

@Component({
  selector: 'app-transaccion-financiera-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './transaccion-financiera-form.component.html',
})
export class TransaccionFinancieraForm {
  private readonly fb = inject(FormBuilder);
  private readonly transaccionService = inject(TransaccionFinancieraService);
  private readonly categoriaService = inject(CategoriaFinancieraService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly id = signal<number | null>(null);
  protected readonly categorias = signal<CategoriaFinanciera[]>([]);
  protected readonly categoriasCargadas = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    concepto: ['', [Validators.required, Validators.maxLength(200)]],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    fechaTransaccion: [new Date().toISOString().substring(0, 10), [Validators.required]],
    tipo: ['INGRESO' as TipoFinanciero, [Validators.required]],
    estado: ['PAGADO' as EstadoFinanciero, [Validators.required]],
    tipoComprobante: ['FACTURA'],
    numeroComprobante: [''],
    categoriaFinancieraId: [0, [Validators.min(1)]],
  });

  constructor() {
    this.cargarCategorias();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.loading.set(true);
      this.transaccionService.obtener(id).subscribe({
        next: (trx) => {
          this.form.patchValue({
            codigo: trx.codigo,
            concepto: trx.concepto,
            monto: trx.monto,
            fechaTransaccion: trx.fechaTransaccion,
            tipo: trx.tipo,
            estado: trx.estado,
            tipoComprobante: trx.tipoComprobante ?? 'FACTURA',
            numeroComprobante: trx.numeroComprobante ?? '',
            categoriaFinancieraId: trx.categoriaFinanciera.id,
          });
          this.loading.set(false);
        },
        error: () => {
          this.errorCarga.set(true);
          this.error.set('No se pudo cargar la transacción financiera.');
          this.loading.set(false);
        },
      });
    }
  }

  cargarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (data) => {
        this.categorias.set(data);
        this.categoriasCargadas.set(true);
      },
      error: () => this.error.set('No se pudieron cargar las categorías financieras.'),
    });
  }

  guardar(): void {
    if (this.loading() || this.errorCarga()) return;
    this.error.set(null);

    const concepto = this.form.controls.concepto;
    concepto.setValue(concepto.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const id = this.id();
    const peticion = id
      ? this.transaccionService.actualizar(id, valor)
      : this.transaccionService.crear(valor);

    this.loading.set(true);
    peticion.subscribe({
      next: () => this.router.navigate(['/finanzas/transacciones']),
      error: (err: HttpErrorResponse) => this.manejarErrorGuardado(err),
    });
  }

  cancelar(): void {
    this.router.navigate(['/finanzas/transacciones']);
  }

  private manejarErrorGuardado(err: HttpErrorResponse): void {
    const mensaje: string = err.error?.message ?? '';

    if (err.status === 404 && mensaje.toLowerCase().includes('categor')) {
      this.error.set('La categoría financiera seleccionada ya no existe. Elige otra de la lista.');
      this.form.controls.categoriaFinancieraId.setValue(0);
      this.cargarCategorias();
    } else if (err.status === 400) {
      this.error.set(mensaje || 'Los datos enviados no son válidos. Revisa los campos del formulario.');
    } else {
      this.error.set('No se pudo guardar la transacción financiera.');
    }
    this.loading.set(false);
  }

  protected mensajeValidacion(campo: 'codigo' | 'concepto' | 'monto' | 'categoriaFinancieraId'): string {
    const control = this.form.controls[campo];

    if (!control.touched) return '';

    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }

    if (control.hasError('maxlength')) {
      return `Máximo ${control.getError('maxlength').requiredLength} caracteres.`;
    }

    if (control.hasError('min')) {
      return campo === 'categoriaFinancieraId'
        ? 'Selecciona una categoría financiera.'
        : 'Debe ser mayor a 0.';
    }

    return '';
  }
}
