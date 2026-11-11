import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { CategoriaService } from '../../categoria/services/categoria.service';
import { Categoria } from '../../categoria/models/categoria.model';
import { ProductoService } from '../services/producto.service';
import { Producto } from '../models/producto.model';

function entero(control: AbstractControl): ValidationErrors | null {
  const val: unknown = control.value;
  return Number.isInteger(Number(val)) ? null : { entero: true };
}

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './producto-form.component.html',
  styleUrl: './producto-form.component.css',
})
export class ProductoForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly productoService = inject(ProductoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly id = signal<number | null>(null);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly categoriasCargadas = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    precio: [0, [Validators.required, Validators.min(0.01)]],
    stock: [0, [Validators.required, Validators.min(0), entero]],
    imagenUrl: [''],
    categoriaId: [0, [Validators.min(1)]],
  });

  ngOnInit(): void {
    this.cargarCategorias();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const prodId = Number(idParam);
      this.id.set(prodId);
      this.loading.set(true);
      this.productoService.obtener(prodId).subscribe({
        next: (producto: Producto) => {
          this.form.patchValue({
            nombre: producto.nombre,
            precio: producto.precio,
            stock: producto.stock,
            imagenUrl: producto.imagenUrl || '',
            categoriaId: producto.categoria ? producto.categoria.id : 0,
          });
          this.loading.set(false);
        },
        error: () => {
          this.errorCarga.set(true);
          this.error.set('No se pudo cargar la información del producto.');
          this.loading.set(false);
        },
      });
    }
  }

  cargarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (data: Categoria[]) => {
        this.categorias.set(data);
        this.categoriasCargadas.set(true);
      },
      error: () => {
        this.error.set('No se pudieron cargar las categorías desde el backend.');
      },
    });
  }

  guardar(): void {
    if (this.loading() || this.errorCarga()) return;
    this.error.set(null);

    const nombreCtrl = this.form.controls.nombre;
    nombreCtrl.setValue(nombreCtrl.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const id = this.id();
    const request = {
      nombre: valor.nombre,
      precio: Number(valor.precio),
      stock: Number(valor.stock),
      imagenUrl: valor.imagenUrl?.trim() || undefined,
      categoriaId: Number(valor.categoriaId),
    };

    const peticion = id
      ? this.productoService.actualizar(id, request)
      : this.productoService.crear(request);

    this.loading.set(true);
    peticion.subscribe({
      next: () => this.router.navigate(['/catalogo/productos']),
      error: (err: HttpErrorResponse) => this.manejarErrorGuardado(err),
    });
  }

  private manejarErrorGuardado(err: HttpErrorResponse): void {
    const mensaje = (err.error as { message?: string } | null)?.message ?? '';

    if (err.status === 404 && mensaje.toLowerCase().includes('categor')) {
      this.error.set('La categoría seleccionada ya no existe en la base de datos. Por favor elija otra.');
      this.form.controls.categoriaId.setValue(0);
      this.cargarCategorias();
    } else if (err.status === 400) {
      this.error.set(mensaje || 'Los datos enviados no son válidos. Revise los campos del formulario.');
    } else {
      this.error.set('No se pudo guardar el producto en el catálogo.');
    }
    this.loading.set(false);
  }

  cancelar(): void {
    this.router.navigate(['/catalogo/productos']);
  }

  protected mensajeValidacion(campo: 'nombre' | 'precio' | 'stock' | 'categoriaId'): string {
    const control = this.form.controls[campo];
    if (!control.touched) return '';

    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) {
      const maxErr = control.getError('maxlength') as { requiredLength: number } | null;
      return `Máximo ${maxErr?.requiredLength ?? 0} caracteres.`;
    }
    if (control.hasError('min')) {
      return campo === 'categoriaId' ? 'Selecciona una categoría válida.' : 'Debe ser mayor o igual a 0.';
    }
    if (control.hasError('entero')) return 'El stock debe ser un número entero.';
    return '';
  }
}
