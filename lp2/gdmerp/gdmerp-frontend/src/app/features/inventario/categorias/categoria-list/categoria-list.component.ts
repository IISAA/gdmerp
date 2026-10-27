import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CategoriaService } from '../services/categoria.service';
import { Categoria, CategoriaRequest } from '../models/categoria.model';

@Component({
  selector: 'app-categoria-list',
  standalone: true,
  imports: [
    CommonModule,
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
  templateUrl: './categoria-list.component.html',
  styleUrls: ['./categoria-list.component.css']
})
export class CategoriaListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoriaService = inject(CategoriaService);
  private readonly snackBar = inject(MatSnackBar);

  // --------------------------------------------------------------------------
  // Señales de Estado Reactivo
  // --------------------------------------------------------------------------
  readonly categorias = signal<Categoria[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly textoBusqueda = signal<string>('');

  // Control del Cajón Deslizante (Side Drawer) y Elemento en Edición
  readonly isDrawerOpen = signal<boolean>(false);
  readonly selectedCategoria = signal<Categoria | null>(null);

  // Señales computadas
  readonly isEditing = computed<boolean>(() => this.selectedCategoria() !== null);
  readonly drawerTitle = computed<string>(() =>
    this.selectedCategoria() ? 'Editar Categoría' : 'Nueva Categoría'
  );

  readonly categoriasFiltradas = computed<Categoria[]>(() => {
    const termino = this.textoBusqueda().toLowerCase().trim();
    if (!termino) {
      return this.categorias();
    }
    return this.categorias().filter((cat) =>
      (cat.nombre && cat.nombre.toLowerCase().includes(termino)) ||
      (cat.descripcion && cat.descripcion.toLowerCase().includes(termino))
    );
  });

  // Formulario Reactivo dentro del Sidenav
  readonly form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    descripcion: ['', [Validators.maxLength(200)]]
  });

  ngOnInit(): void {
    this.cargarCategorias();
  }

  // --------------------------------------------------------------------------
  // Operaciones de Carga y Búsqueda
  // --------------------------------------------------------------------------
  cargarCategorias(): void {
    this.isLoading.set(true);
    this.categoriaService.listar().subscribe({
      next: (data) => {
        this.categorias.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error al listar categorías:', err);
        this.isLoading.set(false);
        this.snackBar.open('❌ No se pudieron cargar las categorías.', 'Cerrar', {
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
    this.selectedCategoria.set(null);
    this.form.reset({ nombre: '', descripcion: '' });
    this.isDrawerOpen.set(true);
  }

  abrirEditar(categoria: Categoria): void {
    this.selectedCategoria.set(categoria);
    this.form.patchValue({
      nombre: categoria.nombre,
      descripcion: categoria.descripcion || ''
    });
    this.isDrawerOpen.set(true);
  }

  cerrarDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedCategoria.set(null);
    this.form.reset({ nombre: '', descripcion: '' });
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
    const request: CategoriaRequest = {
      nombre: raw.nombre.trim(),
      descripcion: raw.descripcion ? raw.descripcion.trim() : ''
    };

    const categoriaActual = this.selectedCategoria();
    const operacion$ = categoriaActual
      ? this.categoriaService.actualizar(categoriaActual.id, request)
      : this.categoriaService.crear(request);

    operacion$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.snackBar.open(
          `✅ Categoría ${categoriaActual ? 'actualizada' : 'creada'} exitosamente.`,
          'Cerrar',
          {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          }
        );
        this.cerrarDrawer();
        this.cargarCategorias();
      },
      error: (err) => {
        this.isSaving.set(false);
        console.error('Error al guardar categoría:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ Error: ${mensajeBackend}`
          : '❌ Ocurrió un error al guardar la categoría. Verifica que el nombre no esté duplicado.';

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
  eliminar(categoria: Categoria): void {
    const confirmacion = confirm(
      `¿Deseas eliminar la categoría "${categoria.nombre}"?\n` +
      `Nota: Si la categoría contiene productos asociados, la operación será rechazada por integridad referencial.`
    );

    if (!confirmacion) {
      return;
    }

    this.categoriaService.eliminar(categoria.id).subscribe({
      next: () => {
        this.categorias.update((actuales) => actuales.filter((c) => c.id !== categoria.id));
        this.snackBar.open(`✅ Categoría "${categoria.nombre}" eliminada correctamente.`, 'Cerrar', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        console.error('Error al eliminar categoría:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ ${mensajeBackend}`
          : '❌ No se pudo eliminar la categoría. Probablemente ya está asignada a uno o más productos.';

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
