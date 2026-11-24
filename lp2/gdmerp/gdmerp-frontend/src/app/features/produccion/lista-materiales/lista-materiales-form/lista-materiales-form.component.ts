import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

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
import { ListaMaterialesService } from '../service/lista-materiales.service';
import { ProductoService } from '../../../inventario/productos/services/producto.service';
import { ProductoResponse } from '../../../inventario/productos/models/producto.model';
import { ListaMaterialesRequest, ListaMaterialesResponse } from '../model/lista-materiales.model';

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
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly listaMaterialesService = inject(ListaMaterialesService);
  private readonly productoService = inject(ProductoService);

  // Estados Reactivos con Signals
  readonly productos = signal<ProductoResponse[]>([]);
  readonly cargandoProductos = signal<boolean>(true);
  readonly cargandoDetalle = signal<boolean>(false);
  readonly cargandoCodigo = signal<boolean>(false);
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

  // Código sin Validators.required para permitir autogeneración en backend si se deja vacío
  readonly form: FormGroup = this.fb.group({
    codigo: ['', [Validators.maxLength(50)]],
    productoTerminadoId: [null, [Validators.required]],
    cantidadBase: [1, [Validators.required, Validators.min(0.0001)]],
    detalles: this.fb.array([], [Validators.required])
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  ngOnInit(): void {
    const paramId = this.route.snapshot.paramMap.get('id');
    const isViewMode =
      this.route.snapshot.queryParamMap.get('modo') === 'ver' ||
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

    this.cargarProductos();
    this.escucharCambioProductoParaCodigo();
  }

  private escucharCambioProductoParaCodigo(): void {
    if (this.isEditMode() || this.isReadOnly()) return;

    this.form.get('productoTerminadoId')?.valueChanges.pipe(
      distinctUntilChanged(),
      filter((id): id is number => Number(id) > 0),
      filter(() => !this.form.get('codigo')?.dirty),
      tap(() => this.cargandoCodigo.set(true)),
      switchMap((productoId) =>
        this.listaMaterialesService.sugerirCodigo(Number(productoId)).pipe(
          catchError(() => of(null))
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((res) => {
      this.cargandoCodigo.set(false);
      if (res?.codigoSugerido) {
        this.form.get('codigo')?.setValue(res.codigoSugerido, { emitEvent: false });
      }
    });
  }

  regenerarCodigo(): void {
    const productoId = Number(this.form.get('productoTerminadoId')?.value);
    if (!productoId || productoId <= 0 || this.isEditMode() || this.isReadOnly()) return;

    this.cargandoCodigo.set(true);
    this.form.get('codigo')?.markAsPristine();

    this.listaMaterialesService.sugerirCodigo(productoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ codigoSugerido }) => {
          this.form.get('codigo')?.setValue(codigoSugerido, { emitEvent: false });
          this.cargandoCodigo.set(false);
        },
        error: () => {
          this.cargandoCodigo.set(false);
          this.mostrarMensaje('No se pudo sugerir el código automáticamente.', 'warning');
        }
      });
  }

  private cargarProductos(): void {
    this.cargandoProductos.set(true);
    this.productoService.listar()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.productos.set(data);
          this.cargandoProductos.set(false);
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
    this.listaMaterialesService.obtenerPorId(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (receta: ListaMaterialesResponse) => {
          this.form.patchValue({
            codigo: receta.codigo,
            productoTerminadoId: receta.productoTerminadoId,
            cantidadBase: receta.cantidadBase
          }, { emitEvent: false });

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

    const formValue = this.form.getRawValue();
    const productoTerminadoId = Number(formValue.productoTerminadoId);
    const detallesMapeados = formValue.detalles.map((d: { insumoId: number; cantidadRequerida: number }) => ({
      insumoId: Number(d.insumoId),
      cantidadRequerida: Number(d.cantidadRequerida)
    }));

    // Validar que no haya insumos duplicados ni auto-referencia antes de ir al backend
    const idsVistos = new Set<number>();
    for (const det of detallesMapeados) {
      if (det.insumoId === productoTerminadoId) {
        this.mostrarMensaje('Un producto terminado no puede ser insumo de su propia receta.', 'warning');
        return;
      }
      if (idsVistos.has(det.insumoId)) {
        this.mostrarMensaje('Existen insumos duplicados en la receta. Combina sus cantidades en una sola fila.', 'warning');
        return;
      }
      idsVistos.add(det.insumoId);
    }

    this.isSubmitting.set(true);

    const codigoLimpio = formValue.codigo ? String(formValue.codigo).trim().toUpperCase() : undefined;
    const request: ListaMaterialesRequest = {
      codigo: codigoLimpio || undefined,
      productoTerminadoId,
      cantidadBase: Number(formValue.cantidadBase),
      detalles: detallesMapeados
    };

    const peticion$ = this.isEditMode() && this.recetaId()
      ? this.listaMaterialesService.actualizar(this.recetaId()!, request)
      : this.listaMaterialesService.crear(request);

    peticion$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (resp) => {
        this.isSubmitting.set(false);
        const accion = this.isEditMode() ? 'actualizada' : 'creada';
        this.mostrarMensaje(`Lista de Materiales "${resp.codigo}" ${accion} exitosamente.`, 'success');
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

  private mostrarMensaje(mensaje: string, tipo: 'success' | 'error' | 'warning'): void {
    const panelClass =
      tipo === 'success' ? ['snackbar-success'] : tipo === 'error' ? ['snackbar-error'] : ['snackbar-warning'];
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass
    });
  }
}