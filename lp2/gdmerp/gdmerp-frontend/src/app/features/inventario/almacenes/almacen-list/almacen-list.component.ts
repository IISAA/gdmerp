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
import { AlmacenService } from '../services/almacen.service';
import { Almacen, AlmacenResponse, AlmacenRequest } from '../models/almacen.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-almacen-list',
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
  templateUrl: './almacen-list.component.html',
  styleUrls: ['./almacen-list.component.css']
})
export class AlmacenListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly almacenService = inject(AlmacenService);
  private readonly snackBar = inject(MatSnackBar);

  // --------------------------------------------------------------------------
  // Señales de Estado Reactivo
  // --------------------------------------------------------------------------
  readonly almacenes = signal<Almacen[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly textoBusqueda = signal<string>('');

  // Control del Cajón Deslizante (Side Drawer) y Elemento en Edición
  readonly isDrawerOpen = signal<boolean>(false);
  readonly selectedAlmacen = signal<Almacen | null>(null);

  // Señales computadas derivadas
  readonly isEditing = computed<boolean>(() => this.selectedAlmacen() !== null);
  readonly drawerTitle = computed<string>(() =>
    this.selectedAlmacen() ? 'Editar Almacén' : 'Nuevo Almacén'
  );

  readonly almacenesFiltrados = computed<Almacen[]>(() => {
    const lista = this.almacenes() || [];
    const termino = this.textoBusqueda().toLowerCase().trim();
    if (!termino) {
      return lista;
    }
    return lista.filter((alm) =>
      (alm.nombre && alm.nombre.toLowerCase().includes(termino)) ||
      (alm.ubicacion && alm.ubicacion.toLowerCase().includes(termino))
    );
  });

  // Formulario Reactivo dentro del Sidenav
  readonly form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    ubicacion: ['', [Validators.maxLength(200)]],
    capacidad: [null, [Validators.min(0)]]
  });

  ngOnInit(): void {
    this.cargarAlmacenes();
  }

  // --------------------------------------------------------------------------
  // Operaciones de Carga y Búsqueda
  // --------------------------------------------------------------------------
  cargarAlmacenes(): void {
    this.isLoading.set(true);
    this.almacenService.listar().subscribe({
      next: (response: unknown) => {
        let lista: AlmacenResponse[] = [];
        if (Array.isArray(response)) {
          lista = response as AlmacenResponse[];
        } else if (response && typeof response === 'object') {
          const resObj = response as Record<string, unknown>;
          if (Array.isArray(resObj['content'])) {
            lista = resObj['content'] as AlmacenResponse[];
          } else if (Array.isArray(resObj['data'])) {
            lista = resObj['data'] as AlmacenResponse[];
          } else if (Array.isArray(resObj['almacenes'])) {
            lista = resObj['almacenes'] as AlmacenResponse[];
          }
        }
        console.log(`[AlmacenListComponent] Almacenes cargados: ${lista.length}`);
        this.almacenes.set(lista);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al listar almacenes:', err);
        this.almacenes.set([]);
        this.isLoading.set(false);
        this.snackBar.open('❌ No se pudieron cargar los almacenes.', 'Cerrar', {
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
    this.selectedAlmacen.set(null);
    this.form.reset({ nombre: '', ubicacion: '', capacidad: null });
    this.isDrawerOpen.set(true);
  }

  abrirEditar(almacen: Almacen): void {
    this.selectedAlmacen.set(almacen);
    this.form.patchValue({
      nombre: almacen.nombre,
      ubicacion: almacen.ubicacion || '',
      capacidad: almacen.capacidad ?? null
    });
    this.isDrawerOpen.set(true);
  }

  cerrarDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedAlmacen.set(null);
    this.form.reset({ nombre: '', ubicacion: '', capacidad: null });
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
    const request: AlmacenRequest = {
      nombre: raw.nombre.trim(),
      ubicacion: raw.ubicacion ? raw.ubicacion.trim() : '',
      capacidad: raw.capacidad !== null && raw.capacidad !== '' ? Number(raw.capacidad) : undefined
    };

    const almacenActual = this.selectedAlmacen();
    const operacion$ = almacenActual
      ? this.almacenService.actualizar(almacenActual.id, request)
      : this.almacenService.crear(request);

    operacion$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.snackBar.open(
          `✅ Almacén ${almacenActual ? 'actualizado' : 'creado'} exitosamente.`,
          'Cerrar',
          {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          }
        );
        this.cerrarDrawer();
        this.cargarAlmacenes(); // Recarga reactiva de la tabla
      },
      error: (err) => {
        this.isSaving.set(false);
        console.error('Error al guardar almacén:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ Error: ${mensajeBackend}`
          : '❌ Ocurrió un error al guardar el almacén. Verifica que el nombre no esté duplicado.';

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
  eliminar(almacen: Almacen): void {
    const confirmacion = confirm(
      `¿Deseas eliminar el almacén "${almacen.nombre}"?\n` +
      `Nota: Si el almacén contiene stock o transacciones activas, la operación será rechazada por el sistema.`
    );

    if (!confirmacion) {
      return;
    }

    this.almacenService.eliminar(almacen.id).subscribe({
      next: () => {
        this.almacenes.update((actuales) => actuales.filter((a) => a.id !== almacen.id));
        this.snackBar.open(`✅ Almacén "${almacen.nombre}" eliminado correctamente.`, 'Cerrar', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        console.error('Error al eliminar almacén:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ ${mensajeBackend}`
          : '❌ No se pudo eliminar el almacén. Probablemente tiene productos o movimientos vinculados.';

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
