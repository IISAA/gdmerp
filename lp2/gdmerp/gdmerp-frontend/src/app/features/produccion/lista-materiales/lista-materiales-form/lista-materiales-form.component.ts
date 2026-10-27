import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Services & Models
import { ListaMaterialesService } from '../../services/lista-materiales.service';
import { ProductoService } from '../../../inventario/productos/services/producto.service';
import { ProductoResponse } from '../../../inventario/productos/models/producto.model';
import { ListaMaterialesRequest, ListaMaterialesResponse } from '../../models/lista-materiales.model';

@Component({
  selector: 'app-lista-materiales-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './lista-materiales-form.component.html',
  styleUrls: ['./lista-materiales-form.component.scss']
})
export class ListaMaterialesFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly listaMaterialesService = inject(ListaMaterialesService);
  private readonly productoService = inject(ProductoService);

  // Estados Reactivos con Signals
  readonly productos = signal<ProductoResponse[]>([]);
  readonly cargandoProductos = signal<boolean>(true);
  readonly cargandoDetalle = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly isEditMode = signal<boolean>(false);
  readonly recetaId = signal<number | null>(null);
  readonly isReadOnly = signal<boolean>(false);
  readonly modoEdicion = computed(() => !this.isReadOnly());

  // 1. Productos a fabricar (TERMINADO o EN_PROCESO)
  readonly productosFabricables = computed(() =>
    this.productos().filter(p => p.tipoProducto === 'TERMINADO' || p.tipoProducto === 'EN_PROCESO')
  );

  // 2. Insumos disponibles (MATERIA_PRIMA, EMPAQUE o CONSUMIBLE)
  readonly insumosDisponibles = computed(() =>
    this.productos().filter(p =>
      p.tipoProducto === 'MATERIA_PRIMA' ||
      p.tipoProducto === 'EMPAQUE' ||
      p.tipoProducto === 'CONSUMIBLE'
    )
  );

  // Formulario Reactivo Maestro-Detalle con soporte decimal para recetas alimentarias
  readonly form: FormGroup = this.fb.group({
    codigo: ['', [Validators.required, Validators.maxLength(50)]],
    productoTerminadoId: [null, [Validators.required]],
    cantidadBase: [1, [Validators.required, Validators.min(0.0001)]],
    detalles: this.fb.array([], [Validators.required])
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  ngOnInit(): void {
    this.cargarProductos();

    const paramId = this.route.snapshot.paramMap.get('id');
    const isViewMode = this.route.snapshot.queryParamMap.get('modo') === 'ver' || 
                       this.route.snapshot.queryParamMap.get('readonly') === 'true' ||
                       this.route.snapshot.data['readOnly'] === true;

    if (isViewMode) {
      this.isReadOnly.set(true);
      this.form.disable();
    }

    if (paramId) {
      const id = Number(paramId);
      if (!isNaN(id)) {
        this.isEditMode.set(true);
        this.recetaId.set(id);
        this.cargarRecetaExistente(id);
      }
    }
  }

  private cargarProductos(): void {
    this.cargandoProductos.set(true);
    this.productoService.listar().subscribe({
      next: (data) => {
        this.productos.set(data);
        this.cargandoProductos.set(false);
        // Si no estamos en modo edición y aún no hay detalles, agregamos una primera fila
        if (!this.isEditMode() && this.detalles.length === 0) {
          this.agregarInsumo();
        }
      },
      error: (err) => {
        this.cargandoProductos.set(false);
        this.mostrarMensaje('Error al cargar catálogo de productos', 'error');
        console.error('Error al listar productos para BOM:', err);
      }
    });
  }

  private cargarRecetaExistente(id: number): void {
    this.cargandoDetalle.set(true);
    this.listaMaterialesService.obtenerPorId(id).subscribe({
      next: (receta: ListaMaterialesResponse) => {
        this.form.patchValue({
          codigo: receta.codigo,
          productoTerminadoId: receta.productoTerminadoId,
          cantidadBase: receta.cantidadBase
        });

        // Reconstrucción del FormArray de detalles
        this.detalles.clear();
        if (receta.detalles && receta.detalles.length > 0) {
          receta.detalles.forEach(d => {
            this.detalles.push(this.crearDetalleGroup(d.insumoId, d.cantidadRequerida));
          });
        } else {
          this.agregarInsumo();
        }

        if (this.isReadOnly()) {
          this.form.disable();
        }

        this.cargandoDetalle.set(false);
      },
      error: (err) => {
        this.cargandoDetalle.set(false);
        this.mostrarMensaje('No se pudo cargar la receta de materiales solicitada.', 'error');
        console.error('Error al obtener BOM por ID:', err);
        this.router.navigate(['/produccion/listas-materiales']);
      }
    });
  }

  // Genera un FormGroup individual para cada insumo con validación decimal (min: 0.0001)
  private crearDetalleGroup(insumoId: number | null = null, cantidadRequerida: number = 1): FormGroup {
    return this.fb.group({
      insumoId: [insumoId, [Validators.required]],
      cantidadRequerida: [cantidadRequerida, [Validators.required, Validators.min(0.0001)]]
    });
  }

  agregarInsumo(): void {
    this.detalles.push(this.crearDetalleGroup());
  }

  eliminarInsumo(index: number): void {
    if (this.detalles.length <= 1) {
      this.mostrarMensaje('La receta debe contener al menos un insumo.', 'warning');
      return;
    }
    this.detalles.removeAt(index);
  }

  // Helpers para visualización limpia en <mat-select-trigger> y badges de unidad
  obtenerProductoSeleccionado(): ProductoResponse | undefined {
    const id = this.form.get('productoTerminadoId')?.value;
    return id ? this.productosFabricables().find(p => p.id === Number(id)) : undefined;
  }

  obtenerInsumoPorId(insumoId: number | null): ProductoResponse | undefined {
    if (!insumoId) return undefined;
    return this.insumosDisponibles().find(i => i.id === Number(insumoId));
  }

  obtenerUnidadMedidaInsumo(insumoId: number | null): string {
    const insumo = this.obtenerInsumoPorId(insumoId);
    return insumo?.unidadMedida || 'UND';
  }

  obtenerUnidadMedidaProductoBase(): string {
    const prod = this.obtenerProductoSeleccionado();
    return prod?.unidadMedida || 'unidades';
  }

  obtenerStepCantidadBase(): string {
    const um = this.obtenerUnidadMedidaProductoBase();
    return ['UND', 'CAJ', 'SAC'].includes(um?.toUpperCase() || '') ? '1' : '0.001';
  }

  obtenerStepInsumo(insumoId: number | null): string {
    const um = this.obtenerUnidadMedidaInsumo(insumoId);
    return ['UND', 'CAJ', 'SAC'].includes(um?.toUpperCase() || '') ? '1' : '0.001';
  }

  guardarReceta(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mostrarMensaje('Por favor, completa todos los campos obligatorios.', 'warning');
      return;
    }

    if (this.detalles.length === 0) {
      this.mostrarMensaje('Debes agregar al menos un ingrediente/insumo a la receta.', 'warning');
      return;
    }

    this.isSubmitting.set(true);

    const formValue = this.form.value;
    const request: ListaMaterialesRequest = {
      codigo: String(formValue.codigo).trim().toUpperCase(),
      productoTerminadoId: Number(formValue.productoTerminadoId),
      cantidadBase: Number(formValue.cantidadBase),
      detalles: formValue.detalles.map((d: { insumoId: number; cantidadRequerida: number }) => ({
        insumoId: Number(d.insumoId),
        cantidadRequerida: Number(d.cantidadRequerida)
      }))
    };

    if (this.isEditMode() && this.recetaId()) {
      this.listaMaterialesService.actualizar(this.recetaId()!, request).subscribe({
        next: (resp) => {
          this.isSubmitting.set(false);
          this.mostrarMensaje(`Lista de Materiales "${resp.codigo}" actualizada exitosamente.`, 'success');
          this.router.navigate(['/produccion/listas-materiales']);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const serverMsg = err.error?.message || err.error?.error || 'No fue posible actualizar la receta de materiales.';
          this.mostrarMensaje(serverMsg, 'error');
          console.error('Error al actualizar lista de materiales:', err);
        }
      });
    } else {
      this.listaMaterialesService.crear(request).subscribe({
        next: (resp) => {
          this.isSubmitting.set(false);
          this.mostrarMensaje(`Lista de Materiales "${resp.codigo}" creada exitosamente.`, 'success');
          this.router.navigate(['/produccion/listas-materiales']);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const serverMsg = err.error?.message || err.error?.error || 'No fue posible guardar la receta de materiales.';
          this.mostrarMensaje(serverMsg, 'error');
          console.error('Error al guardar lista de materiales:', err);
        }
      });
    }
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
