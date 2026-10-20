import { Component, OnInit, inject, signal, computed } from '@angular/core';
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

import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion, OrdenProduccionRequest } from '../../models/produccion.models';

import { ProductoService } from '../../services/producto.service';
import { Producto } from '../../models/producto.model';
import { CentroTrabajoService } from '../../services/centro-trabajo.service';
import { CentroTrabajo } from '../../models/centro-trabajo.model';

@Component({
  selector: 'app-crear-orden',
  standalone: true,
  imports: [
    ReactiveFormsModule, 
    RouterLink, 
    MatAutocompleteModule, 
    MatFormFieldModule, 
    MatInputModule, 
    MatSelectModule, 
    MatButtonModule, 
    MatDatepickerModule, 
    MatNativeDateModule,
    MatSnackBarModule
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
  
  readonly estados = Object.values(EstadoOrdenProduccion);
  isSubmitting = false;
  errorMessage = '';

  // ----------------------------------------------------
  // ESTADOS REACTIVOS (Signals) PARA AUTOCOMPLETES
  // ----------------------------------------------------
  protected readonly productosTerminados = signal<Producto[]>([]);
  protected readonly materiasPrimas = signal<Producto[]>([]);
  protected readonly centros = signal<CentroTrabajo[]>([]);

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
    cantidadPlanificada: [100, [Validators.required, Validators.min(1)]],
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

    // 2. Cargar SOLO Materias Primas
    this.productoService.listar('MATERIA_PRIMA').subscribe({
      next: (data) => {
        this.materiasPrimas.set(data);
        if (data.length > 0) {
          // Agregar la primera fila por defecto si es necesario
          this.agregarMateriaPrima(data[0].id, data[0].nombre, 1);
        }
      },
      error: (err) => console.error('Error cargando insumos:', err)
    });

    // Cargar Centros de Trabajo
    this.centroTrabajoService.listar().subscribe({
      next: (data) => this.centros.set(data),
      error: (err) => console.error('Error cargando centros de trabajo:', err)
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
  // MÉTODOS DE AUTOCOMPLETE: MATERIAS PRIMAS (FORMARRAY)
  // ----------------------------------------------------
  agregarMateriaPrima(id: number = 0, nombre: string = '', cantidad: number = 10): void {
    const detalleGroup = this.fb.group({
      materiaPrimaId: [id, [Validators.required, Validators.min(1)]],
      materiaPrimaNombre: [nombre], // Control temporal para el texto del Autocomplete
      cantidadRequerida: [cantidad, [Validators.required, Validators.min(1)]]
    });
    this.detalles.push(detalleGroup);
  }

  eliminarMateriaPrima(index: number): void {
    this.detalles.removeAt(index);
  }

  // Este método filtra las opciones de la lista flotante *solo para la fila actual*
  protected obtenerMateriasPrimasFiltradas(index: number): Producto[] {
    const textoBuscado = this.detalles.at(index).get('materiaPrimaNombre')?.value?.trim().toLowerCase() || '';
    if (!textoBuscado) return this.materiasPrimas();
    return this.materiasPrimas().filter((p) => p.nombre.toLowerCase().includes(textoBuscado));
  }

  // Cuando el usuario escribe sin seleccionar, marcamos el ID como inválido (0)
  protected invalidarMateriaPrimaId(index: number): void {
    this.detalles.at(index).get('materiaPrimaId')?.setValue(0);
  }

  // Cuando selecciona de la lista, atrapamos el ID real de esa fila
  protected onMateriaPrimaSeleccionada(evento: MatAutocompleteSelectedEvent, index: number): void {
    const producto = this.materiasPrimas().find((p) => p.nombre === evento.option.value);
    this.detalles.at(index).get('materiaPrimaId')?.setValue(producto?.id ?? 0);
    this.detalles.at(index).get('materiaPrimaId')?.markAsTouched();
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
        
        // Extraer el mensaje de error del backend (Spring Boot suele enviarlo en err.error.message o err.error.errors)
        let mensajeBackend = err.error?.message;
        if (err.status === 400 && err.error?.errors && Array.isArray(err.error.errors)) {
          mensajeBackend = err.error.errors.map((e: any) => `${e.field}: ${e.defaultMessage}`).join(' | ');
        }
        
        // Mensaje amigable para el usuario
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
      return `${year}-${month}-${day}T08:00:00`;
    }
    const fechaStr = String(fecha);
    if (fechaStr.includes('T')) return fechaStr;
    return `${fechaStr}T08:00:00`;
  }

  private getTodayDateString(): string { return new Date().toISOString().split('T')[0]; }
  private getDefaultEndDateString(): string {
    const date = new Date();
    date.setDate(date.getDate() + 7);
    return date.toISOString().split('T')[0];
  }
}