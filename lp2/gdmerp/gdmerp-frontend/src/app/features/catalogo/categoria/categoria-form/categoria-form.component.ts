import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoriaService } from '../services/categoria.service';
import { Categoria } from '../models/categoria.model';

@Component({
  selector: 'app-categoria-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './categoria-form.component.html',
  styleUrl: './categoria-form.component.css',
})
export class CategoriaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoriaService = inject(CategoriaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly id = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    descripcion: ['', [Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const catId = Number(idParam);
      this.id.set(catId);
      this.loading.set(true);
      this.categoriaService.obtener(catId).subscribe({
        next: (categoria: Categoria) => {
          this.form.patchValue({
            nombre: categoria.nombre,
            descripcion: categoria.descripcion || '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.errorCarga.set(true);
          this.error.set('No se pudo cargar la información de la categoría.');
          this.loading.set(false);
        },
      });
    }
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
    const peticion = id
      ? this.categoriaService.actualizar(id, valor)
      : this.categoriaService.crear(valor);

    this.loading.set(true);
    peticion.subscribe({
      next: () => this.router.navigate(['/catalogo/categorias']),
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message;
        this.error.set(msg || 'No se pudo guardar la categoría.');
        this.loading.set(false);
      },
    });
  }

  protected mensajeValidacion(campo: 'nombre' | 'descripcion'): string {
    const control = this.form.controls[campo];
    if (!control.touched) return '';

    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('maxlength')) {
      const maxErr = control.getError('maxlength') as { requiredLength: number } | null;
      return `Máximo ${maxErr?.requiredLength ?? 0} caracteres.`;
    }
    return '';
  }
}
