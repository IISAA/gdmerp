import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Producto } from '../../../inventario/productos/models/producto.model';
import { ProductoService } from '../../../inventario/productos/services/producto.service';
import { CentroTrabajo } from '../../centro-trabajo/models/centro-trabajo.model';
import { CentroTrabajoService } from '../../centro-trabajo/services/centro-trabajo.service';
import { ListaMaterialesResponse } from '../../lista-materiales/model/lista-materiales.model';
import { EstadoOrdenProduccion, OrdenProduccionRequest } from '../model/produccion.model';
import { ListaMaterialesService } from '../../lista-materiales/service/lista-materiales.service';
import { OrdenProduccionService } from '../service/orden-produccion.service';

const UNIDADES_ENTERAS = ['UND', 'CAJ', 'SAC'];

@Component({
  selector: 'app-crear-orden',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './crear-orden.component.html',
  styleUrls: ['./crear-orden.component.scss'],
})
export class CrearOrdenComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  private readonly ordenService = inject(OrdenProduccionService);
  private readonly productoService = inject(ProductoService);
  private readonly centroTrabajoService = inject(CentroTrabajoService);
  private readonly listaMaterialesService = inject(ListaMaterialesService);

  // Solo estados válidos al crear una orden
  readonly estados: EstadoOrdenProduccion[] = [
    EstadoOrdenProduccion.PLANIFICADO,
    EstadoOrdenProduccion.COMPLETADO,
  ];

  isSubmitting = false;
  errorMessage = '';

  // Catálogos y estado de recetas (BOM)
  protected readonly productosTerminados = signal<Producto[]>([]);
  protected readonly materiasPrimas = signal<Producto[]>([]);
  protected readonly centros = signal<CentroTrabajo[]>([]);
  protected readonly recetasDisponibles = signal<ListaMaterialesResponse[]>([]);
  protected readonly cargandoRecetas = signal(false);
  protected readonly recetaAplicadaAuto = signal(false);

  // Filtros de Autocomplete
  protected readonly productoBusqueda = signal('');
  protected readonly productosFiltrados = computed(() =>
    this.filtrarPorNombre(this.productosTerminados(), this.productoBusqueda())
  );

  protected readonly centroBusqueda = signal('');
  protected readonly centrosFiltrados = computed(() =>
    this.filtrarPorNombre(this.centros(), this.centroBusqueda())
  );

  readonly form: FormGroup = this.fb.group({
    productoTerminadoId: [0, [Validators.required, Validators.min(1)]],
    listaMaterialesId: [null],
    cantidadPlanificada: [100, [Validators.required, Validators.min(0.0001)]],
    horasTrabajadas: [0, [Validators.min(0)]],
    estado: [EstadoOrdenProduccion.PLANIFICADO, [Validators.required]],
    centroTrabajoId: [0, [Validators.required, Validators.min(1)]],
    fechaPlanificadaInicio: [new Date(), [Validators.required]],
    fechaEstimadaFin: [this.sumarDias(new Date(), 7), [Validators.required]],
    detalles: this.fb.array([]),
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  ngOnInit(): void {
    this.cargarCatalogosIniciales();
    this.escucharCambiosDelFormulario();
  }

  // =========================================================================
  // INICIALIZACIÓN Y REACTIVIDAD
  // =========================================================================

  private cargarCatalogosIniciales(): void {
    forkJoin({
      productos: this.productoService.listar(),
      centros: this.centroTrabajoService.listar(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ productos, centros }) => {
          const terminados = productos.filter((p) => p.tipoProducto === 'TERMINADO');
          const insumos = productos.filter((p) => p.tipoProducto !== 'TERMINADO');

          this.productosTerminados.set(terminados);
          this.materiasPrimas.set(insumos.length > 0 ? insumos : productos);
          this.centros.set(centros);

          this.asegurarFilaInicial();
        },
        error: (err) => {
          console.error('Error cargando catálogos iniciales:', err);
          this.mostrarNotificacion('No se pudieron cargar los catálogos iniciales.', true);
        },
      });
  }

  private escucharCambiosDelFormulario(): void {
    this.form
      .get('cantidadPlanificada')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.recalcularCantidadesPorEscala());

    this.form
      .get('productoTerminadoId')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((productoId) => this.cargarRecetasDeProducto(Number(productoId)));
  }

  // =========================================================================
  // RECETAS / LISTAS DE MATERIALES (BOM)
  // =========================================================================

  private cargarRecetasDeProducto(productoId: number): void {
    if (!productoId || productoId <= 0) {
      this.limpiarEstadoRecetas();
      return;
    }

    this.cargandoRecetas.set(true);
    this.listaMaterialesService
      .obtenerPorProducto(productoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (recetas) => {
          this.cargandoRecetas.set(false);
          this.recetasDisponibles.set(recetas ?? []);

          if (recetas?.length) {
            const primera = recetas[0];
            this.form.get('listaMaterialesId')?.setValue(primera.id, { emitEvent: false });
            this.aplicarReceta(primera, true);
          } else {
            this.recetaAplicadaAuto.set(false);
            this.form.get('listaMaterialesId')?.setValue(null, { emitEvent: false });
            this.asegurarFilaInicial();
          }
        },
        error: (err) => {
          console.error('Error al obtener recetas del producto:', err);
          this.cargandoRecetas.set(false);
          this.limpiarEstadoRecetas();
        },
      });
  }

  protected onRecetaSeleccionada(recetaId: number | null): void {
    this.recetaAplicadaAuto.set(false);

    if (!recetaId) {
      const filasManuales = this.detalles.controls.filter((c) => c.get('esManual')?.value === true);
      this.detalles.clear();
      filasManuales.forEach((fila) => this.detalles.push(fila));
      this.asegurarFilaInicial();
      return;
    }

    const receta = this.recetasDisponibles().find((r) => r.id === Number(recetaId));
    if (receta) {
      this.aplicarReceta(receta, false);
    }
  }

  private aplicarReceta(receta: ListaMaterialesResponse, esAuto: boolean): void {
    this.recetaAplicadaAuto.set(esAuto);
    const cantPlanificada = Number(this.form.get('cantidadPlanificada')?.value) || 1;
    const factor = receta.cantidadBase > 0 ? cantPlanificada / receta.cantidadBase : 1;

    const filasManuales = this.detalles.controls.filter((c) => c.get('esManual')?.value === true);
    this.detalles.clear();

    receta.detalles?.forEach((d) => {
      const cantEscalada = +(d.cantidadRequerida * factor).toFixed(4);
      const nombre = d.nombreInsumo || this.obtenerNombreMateriaPrima(d.insumoId);
      this.detalles.push(this.crearFilaDetalle(d.insumoId, nombre, cantEscalada, false, d.cantidadRequerida));
    });

    filasManuales.forEach((f) => this.detalles.push(f));
  }

  private recalcularCantidadesPorEscala(): void {
    const recetaId = this.form.get('listaMaterialesId')?.value;
    const cantPlanificada = Number(this.form.get('cantidadPlanificada')?.value) || 0;
    if (!recetaId || cantPlanificada <= 0) return;

    const receta = this.recetasDisponibles().find((r) => r.id === Number(recetaId));
    if (!receta?.cantidadBase || receta.cantidadBase <= 0) return;

    const factor = cantPlanificada / receta.cantidadBase;

    this.detalles.controls.forEach((ctrl) => {
      if (ctrl.get('esManual')?.value) return;
      const cantBase = Number(ctrl.get('cantidadBaseReceta')?.value) || 0;
      if (cantBase > 0) {
        ctrl.get('cantidadRequerida')?.setValue(+(cantBase * factor).toFixed(4), { emitEvent: false });
      }
    });
  }

  private limpiarEstadoRecetas(): void {
    this.recetasDisponibles.set([]);
    this.recetaAplicadaAuto.set(false);
    this.form.get('listaMaterialesId')?.setValue(null, { emitEvent: false });
  }

  // =========================================================================
  // AUTOCOMPLETES: PRODUCTO TERMINADO Y CENTRO DE TRABAJO
  // =========================================================================

  protected buscarProducto(texto: string): void {
    this.productoBusqueda.set(texto);
    this.form.controls['productoTerminadoId'].setValue(0);
  }

  protected onProductoSeleccionado(evento: MatAutocompleteSelectedEvent): void {
    const producto = this.productosTerminados().find((p) => p.nombre === evento.option.value);
    const ctrl = this.form.controls['productoTerminadoId'];
    ctrl.setValue(producto?.id ?? 0);
    ctrl.markAsTouched();
  }

  protected buscarCentro(texto: string): void {
    this.centroBusqueda.set(texto);
    this.form.controls['centroTrabajoId'].setValue(0);
  }

  protected onCentroSeleccionado(evento: MatAutocompleteSelectedEvent): void {
    const centro = this.centros().find((c) => c.nombre === evento.option.value);
    const ctrl = this.form.controls['centroTrabajoId'];
    ctrl.setValue(centro?.id ?? 0);
    ctrl.markAsTouched();
  }

  // =========================================================================
  // GESTIÓN DE INSUMOS (FORMARRAY)
  // =========================================================================

  protected agregarMateriaPrima(
    id = 0,
    nombre = '',
    cantidad = 10,
    esManual = true,
    cantidadBaseReceta = 0
  ): void {
    this.detalles.push(this.crearFilaDetalle(id, nombre, cantidad, esManual, cantidadBaseReceta));
  }

  protected eliminarMateriaPrima(index: number): void {
    this.detalles.removeAt(index);
  }

  protected marcarFilaComoManual(index: number): void {
    const fila = this.detalles.at(index);
    if (!fila) return;

    fila.patchValue({
      esManual: true,
      cantidadBaseReceta: 0,
    });
  }

  protected obtenerMateriasPrimasFiltradas(index: number): Producto[] {
    const texto = this.detalles.at(index)?.get('materiaPrimaNombre')?.value || '';
    return this.filtrarPorNombre(this.materiasPrimas(), texto);
  }

  protected invalidarMateriaPrimaId(index: number): void {
    const fila = this.detalles.at(index);
    if (!fila) return;
    fila.patchValue({ materiaPrimaId: 0, esManual: true });
    fila.get('materiaPrimaNombre')?.setErrors({ insumoInvalido: true });
  }

  protected onMateriaPrimaSeleccionada(evento: MatAutocompleteSelectedEvent, index: number): void {
    const producto = this.materiasPrimas().find((p) => p.nombre === evento.option.value);
    const fila = this.detalles.at(index);
    if (!fila) return;

    fila.patchValue({ materiaPrimaId: producto?.id ?? 0, esManual: true });
    fila.get('materiaPrimaId')?.markAsTouched();
    fila.get('materiaPrimaNombre')?.setErrors(null);
  }

  protected obtenerStepProducto(): string {
    const prodId = Number(this.form.get('productoTerminadoId')?.value);
    const prod = this.productosTerminados().find((p) => p.id === prodId);
    return this.obtenerStepPorUnidad(prod?.unidadMedida);
  }

  protected obtenerStepMateriaPrima(index: number): string {
    const mpId = Number(this.detalles.at(index)?.get('materiaPrimaId')?.value);
    const mp = this.materiasPrimas().find((p) => p.id === mpId);
    return this.obtenerStepPorUnidad(mp?.unidadMedida);
  }

  // =========================================================================
  // GUARDADO
  // =========================================================================

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mostrarNotificacion('Por favor, completa todos los campos requeridos correctamente.', true);
      return;
    }

    if (this.detalles.length === 0) {
      this.mostrarNotificacion('Debe incluir al menos un insumo en el detalle de la orden.', true);
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const request = this.construirRequest();

    this.ordenService
      .crear(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.mostrarNotificacion('Orden de producción guardada exitosamente.', false);
          this.router.navigate(['/produccion/ordenes']);
        },
        error: (err) => {
          this.isSubmitting = false;
          this.errorMessage = this.extraerMensajeError(err);
          this.mostrarNotificacion(this.errorMessage, true);
        },
      });
  }

  // =========================================================================
  // HELPERS PRIVADOS
  // =========================================================================

  private crearFilaDetalle(
    id: number,
    nombre: string,
    cantidad: number,
    esManual: boolean,
    cantidadBaseReceta: number
  ): FormGroup {
    return this.fb.group({
      materiaPrimaId: [id, [Validators.required, Validators.min(1)]],
      materiaPrimaNombre: [nombre],
      cantidadRequerida: [cantidad, [Validators.required, Validators.min(0.0001)]],
      esManual: [esManual],
      cantidadBaseReceta: [cantidadBaseReceta],
    });
  }

  private asegurarFilaInicial(): void {
    if (this.detalles.length === 0 && this.materiasPrimas().length > 0) {
      const primero = this.materiasPrimas()[0];
      this.agregarMateriaPrima(primero.id, primero.nombre, 10, true, 0);
    }
  }

  private construirRequest(): OrdenProduccionRequest {
    const raw = this.form.getRawValue();
    return {
      productoTerminadoId: Number(raw.productoTerminadoId),
      cantidadPlanificada: Number(raw.cantidadPlanificada),
      horasTrabajadas: raw.horasTrabajadas != null ? Number(raw.horasTrabajadas) : 0,
      estado: raw.estado as EstadoOrdenProduccion,
      centroTrabajoId: Number(raw.centroTrabajoId),
      fechaPlanificadaInicio: this.formatFechaIso(raw.fechaPlanificadaInicio),
      fechaEstimadaFin: this.formatFechaIso(raw.fechaEstimadaFin),
      detalles: (raw.detalles ?? []).map((d: any) => ({
        materiaPrimaId: Number(d.materiaPrimaId),
        cantidadRequerida: Number(d.cantidadRequerida),
      })),
    };
  }

  private filtrarPorNombre<T extends { nombre: string }>(lista: T[], texto: string): T[] {
    const filtro = texto.trim().toLowerCase();
    return filtro ? lista.filter((item) => item.nombre.toLowerCase().includes(filtro)) : lista;
  }

  private obtenerNombreMateriaPrima(insumoId: number): string {
    return this.materiasPrimas().find((p) => p.id === insumoId)?.nombre ?? '';
  }

  private obtenerStepPorUnidad(unidad?: string): string {
    return UNIDADES_ENTERAS.includes(unidad?.toUpperCase() ?? '') ? '1' : '0.001';
  }

  private extraerMensajeError(err: any): string {
    if (err.status === 400 && Array.isArray(err.error?.errors)) {
      return err.error.errors
        .map((e: any) => `${e.campo || e.field || 'Campo'}: ${e.mensaje || e.defaultMessage || e}`)
        .join(' | ');
    }
    return (
      err.error?.message ||
      'No se pudo guardar la orden. Verifica que exista stock disponible para todos los insumos seleccionados.'
    );
  }

  private mostrarNotificacion(mensaje: string, esError: boolean): void {
    this.snackBar.open(mensaje, esError ? 'Entendido' : 'Cerrar', {
      duration: esError ? 6000 : 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: [esError ? 'error-snackbar' : 'success-snackbar'],
    });
  }

  private formatFechaIso(fecha: Date | string | null): string {
    if (!fecha) return '';
    if (typeof fecha === 'string' && fecha.includes('T')) return fecha;

    const d = fecha instanceof Date ? fecha : new Date(fecha);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  private sumarDias(fecha: Date, dias: number): Date {
    const nueva = new Date(fecha);
    nueva.setDate(nueva.getDate() + dias);
    return nueva;
  }
}