import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion, OrdenProduccionRequest } from '../../models/produccion.models';

import { ProductoService } from '../../services/producto.service';
import { Producto } from '../../models/producto.model';
import { CentroTrabajoService } from '../../services/centro-trabajo.service';
import { CentroTrabajo } from '../../models/centro-trabajo.model';
import { ListaMaterialesService } from '../../services/lista-materiales.service';
import { ListaMaterialesResponse } from '../../models/lista-materiales.model';

@Component({
  selector: 'app-crear-orden',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
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
    MatTooltipModule
  ],
  templateUrl: './crear-orden.component.html'
})
export class CrearOrdenComponent implements OnInit {
  private readonly fb: FormBuilder = inject(FormBuilder);
  private readonly ordenService: OrdenProduccionService = inject(OrdenProduccionService);
  private readonly router: Router = inject(Router);
  private readonly snackBar: MatSnackBar = inject(MatSnackBar);
  
  // Servicios Externos
  private readonly productoService: ProductoService = inject(ProductoService);
  private readonly centroTrabajoService: CentroTrabajoService = inject(CentroTrabajoService);
  private readonly listaMaterialesService: ListaMaterialesService = inject(ListaMaterialesService);
  
  readonly estados = Object.values(EstadoOrdenProduccion);
  isSubmitting = false;
  errorMessage = '';

  // ----------------------------------------------------
  // ESTADOS REACTIVOS (Signals) PARA AUTOCOMPLETES Y BOM
  // ----------------------------------------------------
  protected readonly productosTerminados = signal<Producto[]>([]);
  protected readonly materiasPrimas = signal<Producto[]>([]);
  protected readonly centros = signal<CentroTrabajo[]>([]);

  // Señales de Listas de Materiales (BOM)
  protected readonly recetasDisponibles = signal<ListaMaterialesResponse[]>([]);
  protected readonly cargandoRecetas = signal<boolean>(false);
  protected readonly recetaAplicadaAuto = signal<boolean>(false);

  // Autocomplete: Producto Terminado
  protected readonly productoBusqueda = signal('');
  protected readonly productosFiltrados = computed(() => {
    const texto = this.productoBusqueda().trim().toLowerCase();
    if (!texto) return this.productosTerminados();
    return this.productosTerminados().filter((p) => p.nombre.toLowerCase().includes(texto));
  });

  // Autocomplete: Centro de Trabajo
  protected readonly centroBusqueda = signal('');
  protected readonly centrosFiltrados = computed(() => {
    const texto = this.centroBusqueda().trim().toLowerCase();
    if (!texto) return this.centros();
    return this.centros().filter((c) => c.nombre.toLowerCase().includes(texto));
  });

  // ----------------------------------------------------
  // DEFINICIÓN DEL FORMULARIO
  // ----------------------------------------------------
  readonly form: FormGroup = this.fb.group({
    productoTerminadoId: [0, [Validators.required, Validators.min(1)]],
    listaMaterialesId: [null], // Selector opcional de receta sugerida
    cantidadPlanificada: [100, [Validators.required, Validators.min(0.0001)]],
    estado: [EstadoOrdenProduccion.PLANIFICADO, [Validators.required]],
    centroTrabajoId: [0, [Validators.required, Validators.min(1)]],
    fechaPlanificadaInicio: [this.getTodayDateString(), [Validators.required]],
    fechaEstimadaFin: [this.getDefaultEndDateString(), [Validators.required]],
    detalles: this.fb.array([])
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  ngOnInit(): void {
    // 1. Cargar SOLO Productos Terminados
    this.productoService.listar('TERMINADO').subscribe({
      next: (data) => this.productosTerminados.set(data),
      error: (err) => console.error('Error cargando terminados:', err)
    });

    // 2. Cargar Insumos y Materias Primas para autocompletado del operador
    this.productoService.listar().subscribe({
      next: (data) => {
        const insumos = data.filter(p => p.tipoProducto !== 'TERMINADO');
        this.materiasPrimas.set(insumos.length > 0 ? insumos : data);
        if (this.detalles.length === 0 && insumos.length > 0) {
          this.agregarMateriaPrima(insumos[0].id, insumos[0].nombre, 10, true, 0);
        }
      },
      error: (err) => console.error('Error cargando insumos:', err)
    });

    // 3. Cargar Centros de Trabajo
    this.centroTrabajoService.listar().subscribe({
      next: (data) => this.centros.set(data),
      error: (err) => console.error('Error cargando centros de trabajo:', err)
    });

    // 4. Reactividad al cambiar Cantidad Planificada: recalcular insumos vinculados a la receta
    this.form.get('cantidadPlanificada')?.valueChanges.subscribe(() => {
      this.recalcularCantidadesPorEscala();
    });

    // 5. Reactividad al cambiar Producto Terminado: consultar recetas en tiempo real
    this.form.get('productoTerminadoId')?.valueChanges.subscribe((productoId) => {
      this.onProductoIdCambiado(Number(productoId));
    });
  }

  // ----------------------------------------------------
  // FLUJO REACTIVO: RECETAS / LISTAS DE MATERIALES (BOM)
  // ----------------------------------------------------
  private onProductoIdCambiado(productoId: number): void {
    if (!productoId || productoId <= 0) {
      this.recetasDisponibles.set([]);
      this.recetaAplicadaAuto.set(false);
      this.form.get('listaMaterialesId')?.setValue(null, { emitEvent: false });
      return;
    }

    this.cargandoRecetas.set(true);
    this.listaMaterialesService.obtenerPorProducto(productoId).subscribe({
      next: (recetas: ListaMaterialesResponse[]) => {
        this.cargandoRecetas.set(false);
        this.recetasDisponibles.set(recetas || []);

        if (recetas && recetas.length > 0) {
          const primeraReceta = recetas[0];
          this.form.get('listaMaterialesId')?.setValue(primeraReceta.id, { emitEvent: false });
          this.aplicarReceta(primeraReceta, true);
        } else {
          this.recetaAplicadaAuto.set(false);
          this.form.get('listaMaterialesId')?.setValue(null, { emitEvent: false });
          // Si no tiene receta registrada y no hay insumos, agregar una fila vacía para carga manual
          if (this.detalles.length === 0 && this.materiasPrimas().length > 0) {
            this.agregarMateriaPrima(this.materiasPrimas()[0].id, this.materiasPrimas()[0].nombre, 10, true, 0);
          }
        }
      },
      error: (err) => {
        this.cargandoRecetas.set(false);
        this.recetasDisponibles.set([]);
        this.recetaAplicadaAuto.set(false);
        this.form.get('listaMaterialesId')?.setValue(null, { emitEvent: false });
        console.error('Error al obtener recetas asociadas al producto:', err);
      }
    });
  }

  protected onRecetaSeleccionada(recetaId: number | null): void {
    this.recetaAplicadaAuto.set(false);

    if (!recetaId) {
      // Limpiar únicamente las filas que provenían de la receta anterior; conservar filas manuales
      const filasManuales = this.detalles.controls.filter(c => c.get('esManual')?.value === true);
      this.detalles.clear();
      filasManuales.forEach(fila => this.detalles.push(fila));
      if (this.detalles.length === 0 && this.materiasPrimas().length > 0) {
        this.agregarMateriaPrima(this.materiasPrimas()[0].id, this.materiasPrimas()[0].nombre, 10, true, 0);
      }
      return;
    }

    const receta = this.recetasDisponibles().find(r => r.id === Number(recetaId));
    if (receta) {
      this.aplicarReceta(receta, false);
    }
  }

  private aplicarReceta(receta: ListaMaterialesResponse, esAuto: boolean = false): void {
    this.recetaAplicadaAuto.set(esAuto);
    const cantPlanificada = Number(this.form.get('cantidadPlanificada')?.value) || 1;
    const factor = (receta.cantidadBase && receta.cantidadBase > 0) ? (cantPlanificada / receta.cantidadBase) : 1;

    // 1. Conservar los insumos agregados manualmente por el operador
    const filasManuales = this.detalles.controls.filter(c => c.get('esManual')?.value === true);

    // 2. Limpiar FormArray de insumos
    this.detalles.clear();

    // 3. Cargar insumos de la receta con la cantidad escalada según cantidadPlanificada
    if (receta.detalles && receta.detalles.length > 0) {
      receta.detalles.forEach(d => {
        const cantEscalada = +(d.cantidadRequerida * factor).toFixed(4);
        const nombre = d.nombreInsumo || this.obtenerNombreMateriaPrima(d.insumoId);
        this.detalles.push(this.fb.group({
          materiaPrimaId: [d.insumoId, [Validators.required, Validators.min(1)]],
          materiaPrimaNombre: [nombre],
          cantidadRequerida: [cantEscalada, [Validators.required, Validators.min(0.0001)]],
          esManual: [false],
          cantidadBaseReceta: [d.cantidadRequerida]
        }));
      });
    }

    // 4. Restaurar las filas manuales previas (si el operador ya había añadido insumos a mano)
    filasManuales.forEach(f => this.detalles.push(f));
  }

  private recalcularCantidadesPorEscala(): void {
    const recetaId = this.form.get('listaMaterialesId')?.value;
    if (!recetaId) return;

    const receta = this.recetasDisponibles().find(r => r.id === Number(recetaId));
    if (!receta || !receta.cantidadBase || receta.cantidadBase <= 0) return;

    const cantPlanificada = Number(this.form.get('cantidadPlanificada')?.value) || 0;
    if (cantPlanificada <= 0) return;

    const factor = cantPlanificada / receta.cantidadBase;

    // Recalcular ÚNICAMENTE los insumos provenientes de la receta (esManual !== true)
    this.detalles.controls.forEach(ctrl => {
      const esManual = ctrl.get('esManual')?.value;
      if (!esManual) {
        const cantBase = ctrl.get('cantidadBaseReceta')?.value || 0;
        if (cantBase > 0) {
          const nuevaCant = +(cantBase * factor).toFixed(4);
          ctrl.get('cantidadRequerida')?.setValue(nuevaCant, { emitEvent: false });
        }
      }
    });
  }

  // ----------------------------------------------------
  // MÉTODOS DE AUTOCOMPLETE: PRODUCTO TERMINADO
  // ----------------------------------------------------
  protected buscarProducto(texto: string): void {
    this.productoBusqueda.set(texto);
    this.form.controls['productoTerminadoId'].setValue(0);
  }

  protected onProductoSeleccionado(evento: MatAutocompleteSelectedEvent): void {
    const producto = this.productosTerminados().find((p) => p.nombre === evento.option.value);
    this.form.controls['productoTerminadoId'].setValue(producto?.id ?? 0);
    this.form.controls['productoTerminadoId'].markAsTouched();
  }

  // ----------------------------------------------------
  // MÉTODOS DE AUTOCOMPLETE: CENTRO DE TRABAJO
  // ----------------------------------------------------
  protected buscarCentro(texto: string): void {
    this.centroBusqueda.set(texto);
    this.form.controls['centroTrabajoId'].setValue(0);
  }

  protected onCentroSeleccionado(evento: MatAutocompleteSelectedEvent): void {
    const centro = this.centros().find((c) => c.nombre === evento.option.value);
    this.form.controls['centroTrabajoId'].setValue(centro?.id ?? 0);
    this.form.controls['centroTrabajoId'].markAsTouched();
  }

  // ----------------------------------------------------
  // MÉTODOS DE MATERIAS PRIMAS (FORMARRAY)
  // ----------------------------------------------------
  agregarMateriaPrima(
    id: number = 0, 
    nombre: string = '', 
    cantidad: number = 10, 
    esManual: boolean = true, 
    cantidadBaseReceta: number = 0
  ): void {
    const detalleGroup = this.fb.group({
      materiaPrimaId: [id, [Validators.required, Validators.min(1)]],
      materiaPrimaNombre: [nombre],
      cantidadRequerida: [cantidad, [Validators.required, Validators.min(0.0001)]],
      esManual: [esManual],
      cantidadBaseReceta: [cantidadBaseReceta]
    });
    this.detalles.push(detalleGroup);
  }

  eliminarMateriaPrima(index: number): void {
    this.detalles.removeAt(index);
  }

  marcarFilaComoManual(index: number): void {
    const fila = this.detalles.at(index);
    if (fila) {
      fila.patchValue({ esManual: true });
    }
  }

  protected obtenerMateriasPrimasFiltradas(index: number): Producto[] {
    const textoBuscado = this.detalles.at(index).get('materiaPrimaNombre')?.value?.trim().toLowerCase() || '';
    if (!textoBuscado) return this.materiasPrimas();
    return this.materiasPrimas().filter((p) => p.nombre.toLowerCase().includes(textoBuscado));
  }

  protected invalidarMateriaPrimaId(index: number): void {
    const fila = this.detalles.at(index);
    if (fila) {
      fila.patchValue({ materiaPrimaId: 0, esManual: true });
    }
  }

  protected onMateriaPrimaSeleccionada(evento: MatAutocompleteSelectedEvent, index: number): void {
    const producto = this.materiasPrimas().find((p) => p.nombre === evento.option.value);
    const fila = this.detalles.at(index);
    if (fila) {
      fila.patchValue({
        materiaPrimaId: producto?.id ?? 0,
        esManual: true
      });
      fila.get('materiaPrimaId')?.markAsTouched();
    }
  }

  protected obtenerNombreMateriaPrima(insumoId: number): string {
    const mp = this.materiasPrimas().find(p => p.id === insumoId);
    return mp?.nombre || '';
  }

  protected obtenerStepProducto(): string {
    const prodId = Number(this.form.get('productoTerminadoId')?.value);
    const prod = this.productosTerminados().find(p => p.id === prodId);
    return ['UND', 'CAJ', 'SAC'].includes(prod?.unidadMedida?.toUpperCase() || '') ? '1' : '0.001';
  }

  protected obtenerStepMateriaPrima(index: number): string {
    const fila = this.detalles.at(index);
    const mpId = Number(fila?.get('materiaPrimaId')?.value);
    const mp = this.materiasPrimas().find(p => p.id === mpId);
    return ['UND', 'CAJ', 'SAC'].includes(mp?.unidadMedida?.toUpperCase() || '') ? '1' : '0.001';
  }

  // ----------------------------------------------------
  // GUARDADO Y UTILIDADES
  // ----------------------------------------------------
  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('⚠️ Por favor, completa todos los campos requeridos correctamente.', 'Cerrar', { duration: 4000 });
      return;
    }

    if (this.detalles.length === 0) {
      this.snackBar.open('⚠️ Debe incluir al menos un insumo en el detalle de la orden.', 'Cerrar', { duration: 4000 });
      return;
    }
    
    this.isSubmitting = true;
    this.errorMessage = '';
    const raw = this.form.getRawValue();
    
    const request: OrdenProduccionRequest = {
      productoTerminadoId: Number(raw.productoTerminadoId),
      cantidadPlanificada: Number(raw.cantidadPlanificada),
      estado: raw.estado as EstadoOrdenProduccion,
      centroTrabajoId: Number(raw.centroTrabajoId),
      fechaPlanificadaInicio: this.formatFecha(raw.fechaPlanificadaInicio),
      fechaEstimadaFin: this.formatFecha(raw.fechaEstimadaFin),
      detalles: (raw.detalles || []).map((d: any) => ({
        materiaPrimaId: Number(d.materiaPrimaId),
        cantidadRequerida: Number(d.cantidadRequerida)
      }))
    };

    this.ordenService.crear(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.snackBar.open('✅ Orden de producción guardada exitosamente.', 'Cerrar', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
        this.router.navigate(['/produccion/ordenes']);
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Error del backend:', err);
        
        let mensajeBackend = err.error?.message;
        if (err.status === 400 && err.error?.errors && Array.isArray(err.error.errors)) {
          mensajeBackend = err.error.errors
            .map((e: any) => `${e.campo || e.field || 'Campo'}: ${e.mensaje || e.defaultMessage || e}`)
            .join(' | ');
        }
        
        const mensajeMostrar = mensajeBackend 
          ? `❌ Error: ${mensajeBackend}` 
          : '❌ No se pudo guardar la orden. Verifica que exista stock disponible para todos los insumos seleccionados.';

        this.errorMessage = mensajeMostrar;

        this.snackBar.open(mensajeMostrar, 'Entendido', {
          duration: 6000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  private formatFecha(fecha: any): string {
    if (!fecha) return '';
    if (fecha instanceof Date) {
      const year = fecha.getFullYear();
      const month = String(fecha.getMonth() + 1).padStart(2, '0');
      const day = String(fecha.getDate()).padStart(2, '0');
      const hours = String(fecha.getHours()).padStart(2, '0');
      const minutes = String(fecha.getMinutes()).padStart(2, '0');
      const seconds = String(fecha.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    }
    const fechaStr = String(fecha).trim();
    if (fechaStr.includes('T')) return fechaStr;
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    return `${fechaStr}T${hours}:${minutes}:${seconds}`;
  }

  private getTodayDateString(): string { return new Date().toISOString().split('T')[0]; }
  private getDefaultEndDateString(): string {
    const date = new Date();
    date.setDate(date.getDate() + 7);
    return date.toISOString().split('T')[0];
  }
}