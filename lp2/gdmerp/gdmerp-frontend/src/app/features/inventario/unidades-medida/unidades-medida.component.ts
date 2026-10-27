import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule, MatSlideToggleChange } from '@angular/material/slide-toggle';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';

import { UnidadMedidaService } from './services/unidad-medida.service';
import { UnidadMedida, UnidadMedidaRequest, CategoriaUnidadMedida } from './models/unidad-medida.model';

export interface OpcionCategoriaFiltro {
  valor: 'TODAS' | CategoriaUnidadMedida;
  label: string;
  icono: string;
}

/** Información de la unidad base para el asistente visual de factor de conversión */
interface InfoCategoriaBase {
  unidad: string;
  simbolo: string;
  ayuda: string;
  placeholder: string;
  sunatDefault: string;
}

@Component({
  selector: 'app-unidades-medida',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatSidenavModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatDividerModule,
  ],
  templateUrl: './unidades-medida.component.html',
  styleUrls: ['./unidades-medida.component.css']
})
export class UnidadesMedidaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly unidadMedidaService = inject(UnidadMedidaService);
  private readonly snackBar = inject(MatSnackBar);

  // ── Estados Reactivos ────────────────────────────────────────────────────
  readonly unidades = signal<UnidadMedida[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly isSyncing = signal<boolean>(false);
  readonly categoriaFiltro = signal<'TODAS' | CategoriaUnidadMedida>('TODAS');
  readonly textoBusqueda = signal<string>('');
  readonly isDrawerOpen = signal<boolean>(false);
  readonly selectedUnidad = signal<UnidadMedida | null>(null);

  // ── Métricas ─────────────────────────────────────────────────────────────
  readonly totalUnidades = computed(() => this.unidades().length);
  readonly totalActivas = computed(() => this.unidades().filter(u => u.activo).length);

  // ── Columnas de tabla ────────────────────────────────────────────────────
  readonly displayedColumns: string[] = [
    'codigo', 'nombre', 'categoria',
    'factor', 'codigoSunat', 'permiteDecimales', 'activo', 'acciones'
  ];

  // ── Filtros por magnitud ─────────────────────────────────────────────────
  readonly categoriasFiltro: OpcionCategoriaFiltro[] = [
    { valor: 'TODAS',          label: 'Todas las Magnitudes', icono: 'all_inclusive' },
    { valor: 'MASA',           label: 'Masa / Peso',          icono: 'scale'         },
    { valor: 'LONGITUD',       label: 'Longitud',             icono: 'straighten'    },
    { valor: 'VOLUMEN',        label: 'Volumen / Líquidos',   icono: 'water_drop'    },
    { valor: 'SUPERFICIE',     label: 'Superficie / Área',    icono: 'square_foot'   },
    { valor: 'UNIDAD_EMPAQUE', label: 'Unidades y Empaque',   icono: 'inventory_2'   },
  ];

  // ── Opciones de categoría en el formulario (sin asteriscos manuales) ─────
  readonly categoriasFormulario: { valor: CategoriaUnidadMedida; label: string }[] = [
    { valor: 'MASA',           label: 'Masa / Peso'         },
    { valor: 'LONGITUD',       label: 'Longitud'            },
    { valor: 'VOLUMEN',        label: 'Volumen / Capacidad' },
    { valor: 'SUPERFICIE',     label: 'Superficie / Área'   },
    { valor: 'UNIDAD_EMPAQUE', label: 'Unidades y Empaque'  },
  ];

  /** Asistente visual: texto de ayuda dinámico según la categoría seleccionada */
  private readonly infoCategorias: Record<CategoriaUnidadMedida, InfoCategoriaBase> = {
    MASA:           { unidad: 'Kilogramo', simbolo: 'kg',  ayuda: '¿A cuántos Kilogramos (kg) equivale 1 de esta unidad?',          placeholder: 'Ej. 50 para un saco de 50 kg, 0.001 para Gramo', sunatDefault: 'KGM' },
    LONGITUD:       { unidad: 'Metro',     simbolo: 'm',   ayuda: '¿A cuántos Metros (m) equivale 1 de esta unidad?',                placeholder: 'Ej. 0.01 para Centímetro, 100 para Hectómetro',  sunatDefault: 'MTR' },
    VOLUMEN:        { unidad: 'Litro',     simbolo: 'L',   ayuda: '¿A cuántos Litros (L) equivale 1 de esta unidad?',                placeholder: 'Ej. 3.7854 para Galón, 0.001 para Mililitro',   sunatDefault: 'LTR' },
    SUPERFICIE:     { unidad: 'Metro²',    simbolo: 'm²',  ayuda: '¿A cuántos Metros cuadrados (m²) equivale 1 de esta unidad?',     placeholder: 'Ej. 0.0001 para cm², 10000 para Hectárea',       sunatDefault: 'MTK' },
    UNIDAD_EMPAQUE: { unidad: 'Unidad',    simbolo: 'und', ayuda: 'Cantidad de unidades base que contiene (deja 1 para unidad simple).', placeholder: 'Ej. 1 para Unidad, 12 para Docena, 24 para Display', sunatDefault: 'NIU' },
  };

  // ── Lista filtrada computada ──────────────────────────────────────────────
  readonly unidadesFiltradas = computed(() => {
    const data = this.unidades();
    const filtroCat = this.categoriaFiltro();
    const query = this.textoBusqueda().trim().toLowerCase();

    return data.filter(u => {
      const matchCat = filtroCat === 'TODAS' || u.categoria === filtroCat;
      const matchTexto = !query ||
        (u.codigo && u.codigo.toLowerCase().includes(query)) ||
        (u.nombre && u.nombre.toLowerCase().includes(query)) ||
        (u.simbolo && u.simbolo.toLowerCase().includes(query)) ||
        (u.codigoSunat && u.codigoSunat.toLowerCase().includes(query));
      return matchCat && matchTexto;
    });
  });

  // ── Señal reactiva de la categoría seleccionada en el form ───────────────
  readonly categoriaSeleccionada = signal<CategoriaUnidadMedida>('MASA');

  /** Texto de ayuda dinámico para el campo Factor de Conversión */
  readonly ayudaFactor = computed(() =>
    this.infoCategorias[this.categoriaSeleccionada()].ayuda
  );

  readonly placeholderFactor = computed(() =>
    this.infoCategorias[this.categoriaSeleccionada()].placeholder
  );

  readonly simboloBase = computed(() =>
    this.infoCategorias[this.categoriaSeleccionada()].simbolo
  );

  // ── Formulario Reactivo ───────────────────────────────────────────────────
  readonly form: FormGroup = this.fb.group({
    categoria:       ['MASA' as CategoriaUnidadMedida, [Validators.required]],
    nombre:          ['', [Validators.required, Validators.maxLength(80)]],
    codigo:          ['', [Validators.required, Validators.maxLength(20)]],
    simbolo:         ['', [Validators.required, Validators.maxLength(15)]],
    codigoSunat:     ['', [Validators.maxLength(10)]],
    factorConversion:[1,  [Validators.required, Validators.min(0.0001)]],
    permiteDecimales:[true],
    activo:          [true],
  });

  ngOnInit(): void {
    this.cargarUnidades();
    this._suscribirCambioCategoria();
    this._suscribirAutogeneracionCodigo();
  }

  // ── Suscripciones ─────────────────────────────────────────────────────────

  private _suscribirCambioCategoria(): void {
    this.form.get('categoria')?.valueChanges.subscribe((cat: CategoriaUnidadMedida) => {
      this.categoriaSeleccionada.set(cat);
      // Ajusta permiteDecimales automáticamente si no se está editando
      if (!this.selectedUnidad()) {
        const esDiscreta = cat === 'UNIDAD_EMPAQUE';
        this.form.get('permiteDecimales')?.setValue(!esDiscreta, { emitEvent: false });
        // Precarga el código SUNAT por defecto de la categoría
        const sunatActual = this.form.get('codigoSunat')?.value;
        const defaultSunats = Object.values(this.infoCategorias).map(i => i.sunatDefault);
        if (!sunatActual || defaultSunats.includes(sunatActual)) {
          this.form.get('codigoSunat')?.setValue(
            this.infoCategorias[cat].sunatDefault, { emitEvent: false }
          );
        }
      }
    });
  }

  private _suscribirAutogeneracionCodigo(): void {
    this.form.get('nombre')?.valueChanges.subscribe((nombre: string) => {
      // Solo autogenera en modo Nuevo (no en edición)
      if (this.selectedUnidad()) return;

      const nombreLimpio = (nombre || '').trim();
      if (!nombreLimpio) return;

      // Genera código: primeras 3 letras de cada palabra, máx 6 chars
      const palabras = nombreLimpio.toUpperCase().replace(/[^A-Z0-9\s]/g, '').split(/\s+/).filter(Boolean);
      let codigo = '';
      if (palabras.length === 1) {
        codigo = palabras[0].substring(0, 6);
      } else {
        codigo = palabras.map(p => p.substring(0, 3)).join('').substring(0, 6);
      }

      // Genera símbolo: minúsculas, máx 5 chars, sin espacios
      const simbolo = nombreLimpio.toLowerCase()
        .replace(/[^a-z0-9]/g, '')
        .substring(0, 5);

      this.form.patchValue({ codigo, simbolo }, { emitEvent: false });
    });
  }

  // ── Carga de datos ────────────────────────────────────────────────────────

  cargarUnidades(): void {
    this.isLoading.set(true);
    this.unidadMedidaService.obtenerTodas().subscribe({
      next: (res) => {
        this.unidades.set(res || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando unidades de medida:', err);
        this.isLoading.set(false);
        this.mostrarMensaje('No se pudo cargar el catálogo de unidades de medida.', 'error');
      }
    });
  }

  cargarEstandares(): void {
    this.isSyncing.set(true);
    this.unidadMedidaService.cargarEstandares().subscribe({
      next: (resultado) => {
        this.isSyncing.set(false);
        if (resultado.insertadas > 0) {
          this.mostrarMensaje(
            `✅ ${resultado.insertadas} unidades SI/SUNAT cargadas. (${resultado.omitidas} ya existían)`,
            'success'
          );
        } else {
          this.mostrarMensaje('Catálogo ya estaba completo. Ninguna unidad nueva insertada.', 'success');
        }
        this.cargarUnidades();
      },
      error: (err) => {
        this.isSyncing.set(false);
        console.error('Error cargando estándares:', err);
        this.mostrarMensaje('No se pudo sincronizar el catálogo estándar.', 'error');
      }
    });
  }

  // ── Interacción con filtros ───────────────────────────────────────────────

  seleccionarCategoriaFiltro(cat: 'TODAS' | CategoriaUnidadMedida): void {
    this.categoriaFiltro.set(cat);
  }

  onBuscar(evento: Event): void {
    this.textoBusqueda.set((evento.target as HTMLInputElement).value);
  }

  // ── Apertura/cierre del drawer ────────────────────────────────────────────

  abrirNuevo(): void {
    this.selectedUnidad.set(null);
    this.categoriaSeleccionada.set('MASA');
    this.form.reset({
      categoria: 'MASA',
      nombre: '',
      codigo: '',
      simbolo: '',
      codigoSunat: this.infoCategorias['MASA'].sunatDefault,
      factorConversion: 1,
      permiteDecimales: true,
      activo: true,
    });
    this.form.get('codigo')?.enable();
    this.isDrawerOpen.set(true);
  }

  abrirEditar(um: UnidadMedida): void {
    this.selectedUnidad.set(um);
    this.categoriaSeleccionada.set(um.categoria);
    this.form.patchValue({
      categoria: um.categoria,
      nombre: um.nombre,
      codigo: um.codigo,
      simbolo: um.simbolo,
      codigoSunat: um.codigoSunat || '',
      factorConversion: um.factorConversion,
      permiteDecimales: um.permiteDecimales,
      activo: um.activo,
    });
    // En edición, el código no se puede cambiar para evitar inconsistencias
    this.form.get('codigo')?.disable();
    this.isDrawerOpen.set(true);
  }

  cerrarDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedUnidad.set(null);
    this.form.reset();
    this.form.get('codigo')?.enable();
  }

  // ── Cambio de estado (slide-toggle en tabla) ──────────────────────────────

  cambiarEstado(um: UnidadMedida, event: MatSlideToggleChange): void {
    const nuevoEstado = event.checked;
    this.unidadMedidaService.cambiarEstado(um.id, nuevoEstado).subscribe({
      next: () => {
        this.unidades.update(lista =>
          lista.map(item => item.id === um.id ? { ...item, activo: nuevoEstado } : item)
        );
        this.mostrarMensaje(
          `Unidad "${um.codigo}" ${nuevoEstado ? 'activada' : 'desactivada'}.`, 'success'
        );
      },
      error: (err) => {
        console.error('Error al cambiar estado:', err);
        event.source.checked = !nuevoEstado;
        this.mostrarMensaje('No se pudo actualizar el estado de la unidad.', 'error');
      }
    });
  }

  // ── Guardar (crear / actualizar) ──────────────────────────────────────────

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mostrarMensaje('Completa los campos obligatorios correctamente.', 'warning');
      return;
    }

    this.isSaving.set(true);
    const raw = this.form.getRawValue();

    const request: UnidadMedidaRequest = {
      codigo:           raw.codigo.trim().toUpperCase(),
      codigoSunat:      raw.codigoSunat ? raw.codigoSunat.trim().toUpperCase() : undefined,
      nombre:           raw.nombre.trim(),
      simbolo:          raw.simbolo.trim(),
      categoria:        raw.categoria as CategoriaUnidadMedida,
      factorConversion: Number(raw.factorConversion),
      permiteDecimales: Boolean(raw.permiteDecimales),
      activo:           Boolean(raw.activo),
    };

    const unidadExistente = this.selectedUnidad();
    const accion$ = unidadExistente
      ? this.unidadMedidaService.actualizar(unidadExistente.id, request)
      : this.unidadMedidaService.crear(request);

    accion$.subscribe({
      next: (resp) => {
        this.isSaving.set(false);
        this.cerrarDrawer();
        this.cargarUnidades();
        this.mostrarMensaje(`Unidad "${resp.codigo}" guardada exitosamente.`, 'success');
      },
      error: (err) => {
        this.isSaving.set(false);
        const serverMsg = err.error?.message || 'Error al guardar la unidad de medida.';
        this.mostrarMensaje(serverMsg, 'error');
      }
    });
  }

  // ── Helpers de visualización ──────────────────────────────────────────────

  obtenerTextoEquivalencia(um: UnidadMedida): string {
    const factor = Number(um.factor ?? um.factorConversion) || 1;
    if (factor === 1) return '1.0000 (Unidad Base)';
    if (factor > 0 && factor < 1) {
      const inv = 1 / factor;
      const formatted = inv % 1 === 0 ? inv.toFixed(0) : inv.toFixed(2);
      return `Factor: ${factor} (${formatted} ${um.simbolo} = 1 base)`;
    }
    return `Factor: ${factor} (1 ${um.simbolo} = ${factor} base)`;
  }

  obtenerIconoCategoria(cat: CategoriaUnidadMedida): string {
    const mapa: Record<CategoriaUnidadMedida, string> = {
      MASA: 'scale', LONGITUD: 'straighten', VOLUMEN: 'water_drop',
      SUPERFICIE: 'square_foot', UNIDAD_EMPAQUE: 'inventory_2'
    };
    return mapa[cat] ?? 'help_outline';
  }

  obtenerColorCategoria(cat: CategoriaUnidadMedida): string {
    const mapa: Record<CategoriaUnidadMedida, string> = {
      MASA: '#1d4ed8', LONGITUD: '#b45309', VOLUMEN: '#0891b2',
      SUPERFICIE: '#7c3aed', UNIDAD_EMPAQUE: '#047857'
    };
    return mapa[cat] ?? '#64748b';
  }

  private mostrarMensaje(mensaje: string, tipo: 'success' | 'error' | 'warning'): void {
    const panelClass =
      tipo === 'success' ? ['snackbar-success'] :
      tipo === 'error'   ? ['snackbar-error']   : ['snackbar-warning'];
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: 4500,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass,
    });
  }
}
