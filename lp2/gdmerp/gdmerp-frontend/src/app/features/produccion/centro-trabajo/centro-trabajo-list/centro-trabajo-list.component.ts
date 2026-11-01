import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CentroTrabajoService } from '../services/centro-trabajo.service';
import { CentroTrabajo, CentroTrabajoRequest } from '../models/centro-trabajo.model';

@Component({
  selector: 'app-centro-trabajo-list',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    ReactiveFormsModule,
    FormsModule,
    MatSidenavModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './centro-trabajo-list.component.html',
  styleUrls: ['./centro-trabajo-list.component.css']
})
export class CentroTrabajoListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly centroTrabajoService = inject(CentroTrabajoService);
  private readonly snackBar = inject(MatSnackBar);

  // --------------------------------------------------------------------------
  // Señales de Estado Reactivo
  // --------------------------------------------------------------------------
  readonly centros = signal<CentroTrabajo[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly textoBusqueda = signal<string>('');

  // Control del Cajón Deslizante (Side Drawer) y Elemento en Edición
  readonly isDrawerOpen = signal<boolean>(false);
  readonly selectedCentroTrabajo = signal<CentroTrabajo | null>(null);

  // Señales computadas derivadas
  readonly isEditing = computed<boolean>(() => this.selectedCentroTrabajo() !== null);
  readonly drawerTitle = computed<string>(() =>
    this.selectedCentroTrabajo() ? 'Editar Centro de Trabajo' : 'Nuevo Centro de Trabajo'
  );

  readonly centrosFiltrados = computed<CentroTrabajo[]>(() => {
    const termino = this.textoBusqueda().toLowerCase().trim();
    if (!termino) {
      return this.centros();
    }
    return this.centros().filter((c) =>
      (c.nombre && c.nombre.toLowerCase().includes(termino)) ||
      (c.codigo && c.codigo.toLowerCase().includes(termino))
    );
  });

  // Formulario Reactivo dentro del Sidenav
  readonly form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    capacidad: [1, [Validators.required, Validators.min(0)]],
    costoHora: [null, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.cargarCentros();
  }

  // --------------------------------------------------------------------------
  // Operaciones de Carga y Búsqueda
  // --------------------------------------------------------------------------
  cargarCentros(): void {
    this.isLoading.set(true);
    this.centroTrabajoService.listar().subscribe({
      next: (data) => {
        this.centros.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error al listar centros de trabajo:', err);
        this.isLoading.set(false);
        this.snackBar.open('❌ No se pudieron cargar los centros de trabajo.', 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onBuscar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
  }

  // --------------------------------------------------------------------------
  // Gestión del Drawer (Offcanvas / Side Sidenav)
  // --------------------------------------------------------------------------
  abrirNuevo(): void {
    this.selectedCentroTrabajo.set(null);
    this.form.reset({ nombre: '', codigo: '', capacidad: 1, costoHora: null });
    this.isDrawerOpen.set(true);
  }

  abrirEditar(centro: CentroTrabajo): void {
    this.selectedCentroTrabajo.set(centro);
    this.form.patchValue({
      nombre: centro.nombre,
      codigo: centro.codigo || '',
      capacidad: centro.capacidad ?? 1,
      costoHora: centro.costoHora ?? null
    });
    this.isDrawerOpen.set(true);
  }

  cerrarDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedCentroTrabajo.set(null);
    this.form.reset({ nombre: '', codigo: '', capacidad: 1, costoHora: null });
  }

  // --------------------------------------------------------------------------
  // Guardado (Creación / Edición)
  // --------------------------------------------------------------------------
  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('⚠️ Por favor, ingresa los campos requeridos correctamente.', 'Cerrar', {
        duration: 4000
      });
      return;
    }

    this.isSaving.set(true);
    const raw = this.form.getRawValue();
    const request: CentroTrabajoRequest = {
      nombre: raw.nombre.trim(),
      codigo: raw.codigo ? raw.codigo.trim() : undefined,
      capacidad: Number(raw.capacidad) || 0,
      costoHora: raw.costoHora !== null && raw.costoHora !== '' ? Number(raw.costoHora) : undefined
    };

    const centroActual = this.selectedCentroTrabajo();
    const operacion$ = centroActual
      ? this.centroTrabajoService.actualizar(centroActual.id, request)
      : this.centroTrabajoService.crear(request);

    operacion$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.snackBar.open(
          `✅ Centro de trabajo ${centroActual ? 'actualizado' : 'creado'} exitosamente.`,
          'Cerrar',
          {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          }
        );
        this.cerrarDrawer();
        this.cargarCentros(); // Recarga reactiva de la tabla
      },
      error: (err) => {
        this.isSaving.set(false);
        console.error('Error al guardar centro de trabajo:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ Error: ${mensajeBackend}`
          : '❌ Ocurrió un error al guardar el centro de trabajo. Verifica que el nombre no esté duplicado.';

        this.snackBar.open(mensaje, 'Entendido', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  // --------------------------------------------------------------------------
  // Eliminación
  // --------------------------------------------------------------------------
  eliminar(centro: CentroTrabajo): void {
    const confirmacion = confirm(
      `¿Deseas eliminar el centro de trabajo "${centro.nombre}"?\n` +
      `Nota: Si el centro tiene órdenes de producción asociadas, la operación será rechazada por integridad del sistema.`
    );

    if (!confirmacion) {
      return;
    }

    this.centroTrabajoService.eliminar(centro.id).subscribe({
      next: () => {
        this.centros.update((actuales) => actuales.filter((c) => c.id !== centro.id));
        this.snackBar.open(`✅ Centro de trabajo "${centro.nombre}" eliminado correctamente.`, 'Cerrar', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        console.error('Error al eliminar centro de trabajo:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ ${mensajeBackend}`
          : '❌ No se pudo eliminar el centro de trabajo. Probablemente tiene órdenes de producción vinculadas.';

        this.snackBar.open(mensaje, 'Entendido', {
          duration: 6000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }
}
