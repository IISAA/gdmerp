import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion, OrdenProduccionUpdateRequest } from '../../models/produccion.models';
import { ProductoService } from '../../../inventario/productos/services/producto.service';
import { Producto } from '../../../inventario/productos/models/producto.model';

@Component({
  selector: 'app-editar-orden',
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
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './editar-orden.component.html'
})
export class EditarOrdenComponent implements OnInit {
  private readonly fb: FormBuilder = inject(FormBuilder);
  private readonly ordenService: OrdenProduccionService = inject(OrdenProduccionService);
  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly productoService: ProductoService = inject(ProductoService);
  private readonly snackBar: MatSnackBar = inject(MatSnackBar);

  readonly estados = Object.values(EstadoOrdenProduccion);
  protected estadosPermitidos = signal<string[]>([]);
  protected readonly isFinalizado = signal<boolean>(false);
  protected isInsumosBloqueados = signal<boolean>(false);
  protected readonly ordenId = signal<number>(0);
  isSubmitting = false;
  errorMessage = '';

  protected readonly materiasPrimas = signal<Producto[]>([]);
  protected readonly costoOperativo = signal<number>(0);
  protected readonly costoTotal = signal<number>(0);

  readonly form: FormGroup = this.fb.group({
    productoTerminadoId: [0],
    cantidadPlanificada: [100, [Validators.required, Validators.min(0.0001)]],
    horasTrabajadas: [0, [Validators.min(0)]],
    estado: [EstadoOrdenProduccion.PLANIFICADO, [Validators.required]],
    centroTrabajoId: [0, [Validators.required, Validators.min(1)]],
    fechaPlanificadaInicio: ['', [Validators.required]],
    fechaEstimadaFin: ['', [Validators.required]],
    detalles: this.fb.array([])
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  // 1. Convertir los cambios del FormArray a una Señal reactiva
  // Asumiendo que tu FormArray se llama 'detalles'
  protected detallesChanges = toSignal(this.form.get('detalles')!.valueChanges, { initialValue: [] });

  // 2. Métrica Odoo 1: Total de unidades físicas reservadas
  protected totalUnidadesReservadas = computed(() => {
    const detalles = this.detallesChanges();
    if (!detalles || !Array.isArray(detalles)) return 0;
    return detalles.reduce((suma: number, item: any) => {
      const cant = Number(item?.cantidadRequerida ?? item?.cantidad) || 0;
      return suma + cant;
    }, 0);
  });

  // 3. Métrica Odoo 2: Costo Total Proyectado de la Orden
  protected costoTotalProyectado = computed(() => {
    const detalles = this.detallesChanges();
    if (!detalles || !Array.isArray(detalles)) return 0;
    return detalles.reduce((suma: number, item: any) => {
      const cant = Number(item?.cantidadRequerida ?? item?.cantidad) || 0;
      const costo = Number(item?.costoUnitario) || 0;
      return suma + (cant * costo);
    }, 0);
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.ordenId.set(Number(idParam));
      this.cargarOrden(this.ordenId());
    }

    // Cargar catálogo de materias primas para el Autocomplete
    this.productoService.listar('MATERIA_PRIMA').subscribe({
      next: (data) => this.materiasPrimas.set(data),
      error: (err) => console.error('Error cargando insumos:', err)
    });
  }

  cargarOrden(id: number): void {
    this.ordenService.obtenerPorId(id).subscribe({
      next: (orden) => {
        const finalizado = orden.estado === 'COMPLETADO' || orden.estado === 'CANCELADO';
        this.isFinalizado.set(finalizado);

        const insumosBloqueados = orden.estado !== 'PLANIFICADO';
        this.isInsumosBloqueados.set(insumosBloqueados);

        // Regla de estados permitidos según la máquina de estados
        if (orden.estado === 'PLANIFICADO') {
          this.estadosPermitidos.set(['PLANIFICADO', 'EN_PROCESO', 'CANCELADO']);
        } else if (orden.estado === 'EN_PROCESO') {
          this.estadosPermitidos.set(['EN_PROCESO', 'COMPLETADO', 'CANCELADO']);
        } else {
          this.estadosPermitidos.set([orden.estado]); // Para completado/cancelado (solo lectura)
        }

        // 1. Llenamos los datos principales
        this.form.patchValue({
          productoTerminadoId: orden.productoTerminadoId,
          cantidadPlanificada: orden.cantidadPlanificada,
          horasTrabajadas: orden.horasTrabajadas ?? 0,
          estado: orden.estado,
          centroTrabajoId: orden.centroTrabajo ? orden.centroTrabajo.id : 0,
          fechaPlanificadaInicio: orden.fechaPlanificadaInicio ? orden.fechaPlanificadaInicio.split('T')[0] : '',
          fechaEstimadaFin: orden.fechaEstimadaFin ? orden.fechaEstimadaFin.split('T')[0] : ''
        });
        this.costoOperativo.set(orden.costoOperativo ?? 0);
        this.costoTotal.set(orden.costoTotal ?? 0);

        // 2. Reconstruimos los insumos en el FormArray
        this.detalles.clear();

        if (orden.detalles && orden.detalles.length > 0) {
          orden.detalles.forEach((detalle: any) => {
            this.agregarMateriaPrima(
              detalle.materiaPrimaId,
              detalle.nombreMateriaPrima,
              detalle.cantidadRequerida,
              detalle.costoUnitario || 0
            );
          });
        } else {
          // SOLUCIÓN UX: Si la orden viene sin insumos por un error previo,
          // inyectamos una fila vacía para que el usuario no se quede bloqueado si la orden está activa.
          if (!finalizado) {
            this.agregarMateriaPrima();
          }
        }

        if (finalizado) {
          this.form.disable();
        } else {
          this.form.enable();
          if (insumosBloqueados) {
            this.detalles.disable();
          } else {
            this.detalles.enable();
          }
        }
      },
      error: (err) => {
        this.errorMessage = 'Error al cargar los datos de la orden.';
        console.error(err);
      }
    });
  }

  agregarMateriaPrima(
    materiaPrimaId: number = 0,
    materiaPrimaNombre: string = '',
    cantidadRequerida: number = 1,
    costoUnitario: number = 0
  ): void {
    const detalleGroup = this.fb.group({
      materiaPrimaId: [materiaPrimaId, [Validators.required, Validators.min(1)]],
      materiaPrimaNombre: [materiaPrimaNombre],
      cantidadRequerida: [cantidadRequerida, [Validators.required, Validators.min(0.0001)]],
      costoUnitario: [costoUnitario]
    });
    this.detalles.push(detalleGroup);
  }

  eliminarMateriaPrima(index: number): void {
    this.detalles.removeAt(index);
  }

  protected obtenerStepMateriaPrima(index: number): string {
    const fila = this.detalles.at(index);
    const mpId = Number(fila?.get('materiaPrimaId')?.value);
    const mp = this.materiasPrimas().find(p => p.id === mpId);
    return ['UND', 'CAJ', 'SAC'].includes(mp?.unidadMedida?.toUpperCase() || '') ? '1' : '0.001';
  }

  protected obtenerMateriasPrimasFiltradas(index: number): Producto[] {
    const textoBuscado = this.detalles.at(index).get('materiaPrimaNombre')?.value?.trim().toLowerCase() || '';
    if (!textoBuscado) return this.materiasPrimas();
    return this.materiasPrimas().filter((p) => p.nombre.toLowerCase().includes(textoBuscado));
  }

  protected invalidarMateriaPrimaId(index: number): void {
    this.detalles.at(index).get('materiaPrimaId')?.setValue(0);
  }

  protected onMateriaPrimaSeleccionada(evento: any, index: number): void {
    const producto = this.materiasPrimas().find((p) => p.nombre === evento.option.value);
    this.detalles.at(index).get('materiaPrimaId')?.setValue(producto?.id ?? 0);
    if (producto && producto.costoPromedio !== undefined) {
      this.detalles.at(index).get('costoUnitario')?.setValue(producto.costoPromedio);
    }
    this.detalles.at(index).get('materiaPrimaId')?.markAsTouched();
  }

  getEstadoBadgeBg(estado?: string): string {
    switch (estado) {
      case EstadoOrdenProduccion.PLANIFICADO:
      case 'PLANIFICADO':
        return '#dbeafe';
      case EstadoOrdenProduccion.EN_PROCESO:
      case 'EN_PROCESO':
        return '#fef3c7';
      case EstadoOrdenProduccion.COMPLETADO:
      case 'COMPLETADO':
        return '#d1fae5';
      case EstadoOrdenProduccion.CANCELADO:
      case 'CANCELADO':
        return '#fee2e2';
      default:
        return '#f3f4f6';
    }
  }

  getEstadoBadgeColor(estado?: string): string {
    switch (estado) {
      case EstadoOrdenProduccion.PLANIFICADO:
      case 'PLANIFICADO':
        return '#1d4ed8';
      case EstadoOrdenProduccion.EN_PROCESO:
      case 'EN_PROCESO':
        return '#b45309';
      case EstadoOrdenProduccion.COMPLETADO:
      case 'COMPLETADO':
        return '#047857';
      case EstadoOrdenProduccion.CANCELADO:
      case 'CANCELADO':
        return '#b91c1c';
      default:
        return '#374151';
    }
  }

  guardar(): void {
    if (this.isFinalizado()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('⚠️ Por favor, completa todos los campos requeridos correctamente.', 'Cerrar', { duration: 4000 });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    const raw = this.form.getRawValue();

    // Construimos el DTO exacto para el PUT
    const request: OrdenProduccionUpdateRequest = {
      cantidadPlanificada: Number(raw.cantidadPlanificada),
      horasTrabajadas: raw.horasTrabajadas != null ? Number(raw.horasTrabajadas) : 0,
      estado: raw.estado as EstadoOrdenProduccion,
      centroTrabajoId: Number(raw.centroTrabajoId),
      fechaPlanificadaInicio: this.formatFecha(raw.fechaPlanificadaInicio),
      fechaEstimadaFin: this.formatFecha(raw.fechaEstimadaFin),
      detalles: (raw.detalles || []).map((d: any) => ({
        materiaPrimaId: Number(d.materiaPrimaId),
        cantidadRequerida: Number(d.cantidadRequerida)
      }))
    };

    this.ordenService.actualizar(this.ordenId(), request).subscribe({
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
      error: (err: HttpErrorResponse) => {
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

  // Alias para compatibilidad
  actualizar(): void {
    this.guardar();
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
}