import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap, tap, catchError, startWith } from 'rxjs/operators';

import { ProductoService } from '../services/producto.service';
import { TipoProducto } from '../models/producto.model';
import { CategoriaService } from '../../categorias/services/categoria.service';
import { Categoria } from '../../categorias/models/categoria.model';
import { AlmacenService } from '../../almacenes/services/almacen.service';
import { Almacen } from '../../almacenes/models/almacen.model';
import { UnidadMedidaService } from '../../unidades-medida/services/unidad-medida.service';
import { UnidadMedida, CategoriaUnidadMedida } from '../../unidades-medida/models/unidad-medida.model';

export interface OpcionTipoProducto {
  valor: TipoProducto;
  label: string;
}

export interface GrupoUnidadesOpt {
  categoria: CategoriaUnidadMedida;
  nombre: string;
  unidades: UnidadMedida[];
}

@Component({
  selector: 'app-formulario-producto',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCardModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './formulario-producto.component.html',
  styleUrls: ['./formulario-producto.component.css']
})
export class FormularioProductoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly productoService = inject(ProductoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly almacenService = inject(AlmacenService);
  private readonly unidadMedidaService = inject(UnidadMedidaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  // Estados
  readonly isEditMode = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly productoId = signal<number | null>(null);
  readonly skuEditadoManualmente = signal<boolean>(false); // Control de edición manual

  // Catálogos reactivos
  readonly categorias = signal<Categoria[]>([]);
  readonly almacenes = signal<Almacen[]>([]);
  readonly unidadesMedidaCatalogo = signal<UnidadMedida[]>([]);

  // Opciones estandarizadas de roles operativos (Tipo de Producto)
  readonly tiposProducto = signal<OpcionTipoProducto[]>([
    { valor: 'MATERIA_PRIMA', label: 'Materia Prima (Insumo de receta)' },
    { valor: 'EMPAQUE', label: 'Material de Empaque / Envase' },
    { valor: 'EN_PROCESO', label: 'Producto Intermedio / En Proceso' },
    { valor: 'TERMINADO', label: 'Producto Terminado (Para venta/stock final)' },
    { valor: 'CONSUMIBLE', label: 'Consumible / Suministro General' }
  ]);

  readonly mapaCategoriasUnidades: Record<CategoriaUnidadMedida, string> = {
    MASA: 'Peso / Masa',
    VOLUMEN: 'Volumen / Líquidos',
    LONGITUD: 'Longitud',
    SUPERFICIE: 'Superficie / Área',
    UNIDAD_EMPAQUE: 'Unidades y Empaque'
  };

  // Catálogo agrupado dinámico obtenido del backend
  readonly gruposUnidadesMedida = computed<GrupoUnidadesOpt[]>(() => {
    const unidades = this.unidadesMedidaCatalogo();
    const ordenCategorias: CategoriaUnidadMedida[] = [
      'MASA',
      'VOLUMEN',
      'LONGITUD',
      'SUPERFICIE',
      'UNIDAD_EMPAQUE'
    ];

    return ordenCategorias
      .map(cat => ({
        categoria: cat,
        nombre: this.mapaCategoriasUnidades[cat] || cat,
        unidades: unidades.filter(u => u.categoria === cat)
      }))
      .filter(g => g.unidades.length > 0);
  });

  // Lista plana para compatibilidad y búsquedas
  readonly unidadesMedida = computed(() => this.unidadesMedidaCatalogo());

  // Definición del Formulario Reactivo
  readonly form = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(30)]],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    tipoProducto: ['MATERIA_PRIMA' as TipoProducto, Validators.required],
    categoriaId: [null as number | null, Validators.required],
    almacenId: [null as number | null, Validators.required],
    unidadMedida: ['UND', Validators.required],
    stockMinimo: [0, [Validators.required, Validators.min(0)]],
    costoPromedio: [0, [Validators.required, Validators.min(0)]]
  });

  obtenerStepStockMinimo(): string {
    const codigo = this.form.get('unidadMedida')?.value;
    const unidad = this.unidadesMedidaCatalogo().find(u => u.codigo === codigo);
    if (!unidad) {
      return ['UND', 'CAJ', 'SAC', 'ROL', 'PAQ'].includes(codigo || '') ? '1' : '0.1';
    }
    return unidad.permiteDecimales ? '0.1' : '1';
  }

  ngOnInit(): void {
    this.cargarCatalogos();
    this.inicializarGeneradorSku();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.productoId.set(+idParam);
      this.cargarProducto(+idParam);
    }
  }

  private cargarCatalogos(): void {
    this.categoriaService.listar().subscribe({
      next: (data) => this.categorias.set(data),
      error: (err) => console.error('Error cargando catálogo de categorías:', err)
    });

    this.almacenService.listar().subscribe({
      next: (data) => {
        this.almacenes.set(data);
        if (!this.isEditMode() && !this.skuEditadoManualmente()) {
          this.actualizarSugerenciaSku();
        }
      },
      error: (err) => console.error('Error cargando catálogo de almacenes:', err)
    });

    this.unidadMedidaService.listar(true).subscribe({
      next: (data) => this.unidadesMedidaCatalogo.set(data),
      error: (err) => console.error('Error cargando catálogo dinámico de unidades de medida:', err)
    });
  }

  private cargarProducto(id: number): void {
    this.form.disable();
    this.productoService.obtenerPorId(id).subscribe({
      next: (producto) => {
        this.form.patchValue({
          sku: producto.sku,
          nombre: producto.nombre,
          tipoProducto: producto.tipoProducto as TipoProducto,
          categoriaId: producto.categoria ? producto.categoria.id : null,
          almacenId: producto.almacen ? producto.almacen.id : null,
          unidadMedida: this.normalizarUnidadMedida(producto.unidadMedida),
          stockMinimo: producto.stockMinimo,
          costoPromedio: producto.costoPromedio
        });
        this.form.enable();
        this.skuEditadoManualmente.set(true);
        this.form.get('sku')?.disable();
      },
      error: (err) => {
        console.error('Error cargando producto:', err);
        this.mostrarMensaje('❌ Error al cargar los datos del producto');
        this.router.navigate(['/inventario/productos']);
      }
    });
  }

  private normalizarUnidadMedida(unidad: string | null | undefined): string {
    if (!unidad) return 'UND';
    const u = unidad.trim().toUpperCase();
    if (u === 'UDS' || u === 'UNIDADES' || u === 'UND') return 'UND';
    if (u === 'KG' || u === 'KILOGRAMOS' || u === 'KILOGRAMO') return 'KG';
    if (u === 'G' || u === 'GR' || u === 'GRAMOS' || u === 'GRAMO') return 'GR';
    if (u === 'TON' || u === 'TONELADAS' || u === 'TONELADA' || u === 'T') return 'TON';
    if (u === 'QQ' || u === 'QUINTALES' || u === 'QUINTAL') return 'QQ';
    if (u === 'L' || u === 'LT' || u === 'LTS' || u === 'LITROS' || u === 'LITRO') return 'LT';
    if (u === 'ML' || u === 'MILILITROS' || u === 'MILILITRO') return 'ML';
    if (u === 'CAJ' || u === 'CAJAS' || u === 'CAJA') return 'CAJ';
    if (u === 'SAC' || u === 'SACOS' || u === 'SACO') return 'SAC';
    if (u === 'ROL' || u === 'ROLLOS' || u === 'ROLLO') return 'ROL';
    return u;
  }

  regenerarSku(): void {
    if (this.isEditMode()) {
      return;
    }
    this.skuEditadoManualmente.set(false);
    this.actualizarSugerenciaSku();
  }

  // Algoritmo local instantáneo
  generarPrevisualizacionSku(nombreAlmacen: string, nombreProducto: string): string {
    const codAlmacen = this.extraerCodigoAlmacen(nombreAlmacen);
    const codProducto = this.extraerCodigoProducto(nombreProducto);
    return `APF-${codAlmacen}-${codProducto}`;
  }

  extraerCodigoAlmacen(nombreAlmacen: string): string {
    const filtrado = this.limpiarYFiltrarConectores(nombreAlmacen);
    if (!filtrado) {
      return 'ALM';
    }
    const palabras = filtrado.split(/\s+/);
    if (palabras.length >= 2) {
      const limit = Math.min(palabras.length, 3);
      return palabras.slice(0, limit).map(p => p.charAt(0)).join('');
    } else {
      const palabra = palabras[0];
      return palabra.length >= 3 ? palabra.substring(0, 3) : palabra.padEnd(3, 'X');
    }
  }

  extraerCodigoProducto(nombreProducto: string): string {
    const filtrado = this.limpiarYFiltrarConectores(nombreProducto);
    if (!filtrado) {
      return 'PRD';
    }
    const palabras = filtrado.split(/\s+/);
    if (palabras.length >= 2) {
      const limit = Math.min(palabras.length, 4);
      return palabras.slice(0, limit).map(p => p.charAt(0)).join('');
    } else {
      const palabra = palabras[0];
      return palabra.length >= 3 ? palabra.substring(0, 3) : palabra.padEnd(3, 'X');
    }
  }

  limpiarYFiltrarConectores(texto: string): string {
    if (!texto || !texto.trim()) {
      return '';
    }
    const normalizado = texto
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase()
      .replace(/[^A-Z0-9 ]/g, ' ')
      .trim();

    const conectores = new Set(['DE', 'EL', 'LA', 'LOS', 'LAS', 'PARA', 'Y', 'DEL', 'AL', 'EN', 'CON']);

    return normalizado
      .split(/\s+/)
      .filter(w => w.length > 0 && !conectores.has(w))
      .join(' ');
  }

  private inicializarGeneradorSku(): void {
    combineLatest([
      this.form.get('nombre')!.valueChanges.pipe(startWith(this.form.get('nombre')!.value || '')),
      this.form.get('almacenId')!.valueChanges.pipe(startWith(this.form.get('almacenId')!.value))
    ])
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      filter(() => !this.isEditMode() && !this.skuEditadoManualmente()),
      tap(([nombre, almacenId]) => {
        if (this.skuEditadoManualmente()) {
          return;
        }
        const skuActual = this.form.get('sku')?.value ?? '';
        if (!nombre?.trim() || !almacenId) {
          if (!skuActual || skuActual.startsWith('APF-')) {
            this.form.patchValue({ sku: '' }, { emitEvent: false });
          }
          return;
        }
        const almacen = this.almacenes().find(a => a.id === Number(almacenId));
        const nombreAlmacen = almacen ? almacen.nombre : '';
        const prefijo = this.generarPrevisualizacionSku(nombreAlmacen, nombre);
        if (!skuActual || skuActual.endsWith('-....') || !skuActual.startsWith(prefijo)) {
          this.form.patchValue({ sku: `${prefijo}-....` }, { emitEvent: false });
        }
      }),
      debounceTime(300),
      distinctUntilChanged(([n1, a1], [n2, a2]) => n1 === n2 && a1 === a2),
      switchMap(([nombre, almacenId]) => {
        if (!nombre?.trim() || !almacenId || this.skuEditadoManualmente() || this.isEditMode()) {
          return of(null);
        }
        return this.productoService.sugerirSku(Number(almacenId), nombre.trim()).pipe(
          catchError(err => {
            console.error('Error al sugerir SKU:', err);
            return of(null);
          })
        );
      })
    )
    .subscribe(res => {
      if (res && res.skuSugerido && !this.skuEditadoManualmente() && !this.isEditMode()) {
        this.form.patchValue({ sku: res.skuSugerido }, { emitEvent: false });
        this.form.get('sku')?.markAsTouched();
        this.form.get('sku')?.updateValueAndValidity({ emitEvent: false });
      }
    });
  }

  private actualizarSugerenciaSku(): void {
    if (this.isEditMode() || this.skuEditadoManualmente()) {
      return;
    }
    const nombre = this.form.get('nombre')?.value;
    const almacenId = this.form.get('almacenId')?.value;
    if (!nombre?.trim() || !almacenId) {
      return;
    }

    const almacen = this.almacenes().find(a => a.id === Number(almacenId));
    const nombreAlmacen = almacen ? almacen.nombre : '';
    const prefijo = this.generarPrevisualizacionSku(nombreAlmacen, nombre);
    this.form.patchValue({ sku: `${prefijo}-....` }, { emitEvent: false });

    this.productoService.sugerirSku(Number(almacenId), nombre.trim()).subscribe({
      next: (res) => {
        if (res?.skuSugerido && !this.skuEditadoManualmente() && !this.isEditMode()) {
          this.form.patchValue({ sku: res.skuSugerido }, { emitEvent: false });
          this.form.get('sku')?.updateValueAndValidity({ emitEvent: false });
        }
      },
      error: (err) => console.error('Error al sugerir SKU:', err)
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mostrarMensaje('⚠️ Por favor completa los campos requeridos correctamente.');
      return;
    }

    this.isSubmitting.set(true);
    const formData = this.form.getRawValue();
    if (formData.sku) {
      formData.sku = formData.sku.trim().toUpperCase();
    }

    const request$ = this.isEditMode()
      ? this.productoService.actualizar(this.productoId()!, formData as any)
      : this.productoService.crear(formData as any);

    request$.subscribe({
      next: () => {
        this.snackBar.open(
          `✅ Producto ${this.isEditMode() ? 'actualizado' : 'registrado'} con éxito.`,
          'Cerrar',
          {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          }
        );
        this.router.navigate(['/inventario/productos']);
      },
      error: (err) => {
        console.error('Error al guardar producto:', err);
        const mensajeBackend = err.error?.message;
        const mensaje = mensajeBackend
          ? `❌ Error: ${mensajeBackend}`
          : '❌ Error al guardar el producto.';

        this.snackBar.open(mensaje, 'Entendido', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
        this.isSubmitting.set(false);
      }
    });
  }

  private mostrarMensaje(msg: string): void {
    this.snackBar.open(msg, 'Cerrar', {
      duration: 3500,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}
