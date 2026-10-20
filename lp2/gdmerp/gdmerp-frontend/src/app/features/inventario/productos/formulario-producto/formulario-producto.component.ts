import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ProductoService } from '../services/producto.service';
import { CategoriaResponse, AlmacenResponse, TipoProducto } from '../models/producto.model';

@Component({
  selector: 'app-formulario-producto',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatButtonModule, MatFormFieldModule, MatInputModule, 
    MatSelectModule, MatCardModule, MatIconModule, MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './formulario-producto.component.html',
  styleUrls: ['./formulario-producto.component.css']
})
export class FormularioProductoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly productoService = inject(ProductoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  // Estados
  protected isEditMode = signal<boolean>(false);
  protected isSubmitting = signal<boolean>(false);
  protected productoId = signal<number | null>(null);

  // TODO: Estos deberían venir de sus respectivos servicios (CategoriaService, AlmacenService). 
  // Por ahora los simulamos para no romper la compilación de la vista.
  protected categorias = signal<CategoriaResponse[]>([
    { id: 1, nombre: 'Materias Primas', activo: true },
    { id: 2, nombre: 'Productos Terminados', activo: true },
    { id: 3, nombre: 'Empaques', activo: true }
  ]);
  protected almacenes = signal<AlmacenResponse[]>([
    { id: 1, nombre: 'Almacén Principal' },
    { id: 2, nombre: 'Almacén de Producción' }
  ]);
  
  protected tiposProducto = signal<TipoProducto[]>(['MATERIA_PRIMA', 'TERMINADO', 'EMPAQUE', 'EN_PROCESO', 'CONSUMIBLE']);

  // Definición del Formulario
  protected form = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(50)]],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    tipoProducto: ['' as TipoProducto, Validators.required],
    categoriaId: [null as number | null, Validators.required],
    almacenId: [null as number | null, Validators.required],
    unidadMedida: ['uds', Validators.required],
    stockMinimo: [0, [Validators.required, Validators.min(0)]],
    costoPromedio: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.productoId.set(+idParam);
      this.cargarProducto(+idParam);
    }
  }

  private cargarProducto(id: number): void {
    this.form.disable(); // Bloquear mientras carga
    this.productoService.obtenerPorId(id).subscribe({
      next: (producto) => {
        this.form.patchValue({
          sku: producto.sku,
          nombre: producto.nombre,
          tipoProducto: producto.tipoProducto as TipoProducto,
          categoriaId: producto.categoria.id,
          almacenId: producto.almacen.id,
          unidadMedida: producto.unidadMedida,
          stockMinimo: producto.stockMinimo,
          costoPromedio: producto.costoPromedio
        });
        this.form.enable();
        // Nota: En un ERP real, el SKU suele bloquearse tras la creación
        this.form.get('sku')?.disable(); 
      },
      error: (err) => {
        this.mostrarMensaje('Error al cargar el producto');
        this.router.navigate(['/inventario/productos']);
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const formData = this.form.getRawValue(); // Usa getRawValue para incluir campos disabled (como el SKU)

    const request$ = this.isEditMode() 
      ? this.productoService.actualizar(this.productoId()!, formData as any)
      : this.productoService.crear(formData as any);

    request$.subscribe({
      next: () => {
        this.mostrarMensaje(`Producto ${this.isEditMode() ? 'actualizado' : 'registrado'} con éxito`);
        this.router.navigate(['/inventario/productos']);
      },
      error: (err) => {
        console.error(err);
        this.mostrarMensaje('Error al guardar el producto');
        this.isSubmitting.set(false);
      }
    });
  }

  private mostrarMensaje(msg: string): void {
    this.snackBar.open(msg, 'Cerrar', { duration: 3000, horizontalPosition: 'right', verticalPosition: 'top' });
  }
}
