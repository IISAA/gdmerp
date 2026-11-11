import { Component, OnInit, inject, signal, computed, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import {
  FormBuilder,
  FormGroup,
  FormArray,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RendicionCajaChicaService } from '../services/rendicion-caja-chica.service';
import {
  AsignacionCajaCentroCosto,
  CajaChica,
  CentroCosto,
  RendicionCajaChica,
  RendicionCajaChicaRequest,
} from '../models/rendicion-caja-chica.model';
import { ProductoService } from '../../catalogo/producto/producto.service';
import { Producto } from '../../catalogo/producto/producto.model';

export interface FormLineaItem {
  concepto: string;
  cantidad: number;
  precioUnitario: number;
  productoId: number | null;
  centroCostoId: number | null;
}

@Component({
  selector: 'app-rendicion-caja-chica-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './rendicion-caja-chica-form.component.html',
  styleUrls: ['./rendicion-caja-chica-form.component.css'],
})
export class RendicionCajaChicaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly rendicionService = inject(RendicionCajaChicaService);
  private readonly productoService = inject(ProductoService);
  private readonly router = inject(Router);

  // Señales de Estado
  protected readonly cajasChicas = signal<CajaChica[]>([]);
  protected readonly productos = signal<Producto[]>([]);
  protected readonly centrosCosto = signal<CentroCosto[]>([]);
  protected readonly presupuestosCaja = signal<AsignacionCajaCentroCosto[]>([]);
  protected readonly loading = signal(false);
  protected readonly cargandoDatos = signal(true);
  protected readonly mensajeExito = signal<string | null>(null);
  protected readonly intentoEnvioInvalido = signal(false);

  // Errores diferenciados del backend
  protected readonly errorSolvencia = signal<string | null>(null);
  protected readonly errorDuplicidad = signal<string | null>(null);
  protected readonly errorGeneral = signal<string | null>(null);

  // Control de Modal de Confirmación
  protected readonly mostrarModalConfirmacion = signal(false);

  // Control de Autocompletado por línea
  protected readonly buscadorFiltro = signal<{ [index: number]: string }>({});
  protected readonly dropdownAbierto = signal<number | null>(null);

  // Sincronización reactiva con Signals para Cálculos Matemáticos Derivados (computed)
  protected readonly lineasSignal = signal<FormLineaItem[]>([]);

  // Formulario Reactivo Tipado con nonNullable
  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(50)]],
    responsable: ['', [Validators.required, Validators.maxLength(100)]],
    cajaChicaId: [0, [Validators.required, Validators.min(1)]],
    detalles: this.fb.array<FormGroup>([]),
  });

  // Signal Derivada: Caja Chica actualmente seleccionada
  protected readonly cajaSeleccionada = computed(() => {
    const id = Number(this.form.get('cajaChicaId')?.value);
    return this.cajasChicas().find((c) => c.id === id) ?? null;
  });

  // Signal Derivada: Subtotales por línea (precio * cantidad) recalculados en vivo
  protected readonly subtotales = computed(() => {
    return this.lineasSignal().map((linea) => {
      const cant = Number(linea.cantidad) || 0;
      const precio = Number(linea.precioUnitario) || 0;
      return cant * precio;
    });
  });

  // Signal Derivada: Total General recalculado reactivamente con computed
  protected readonly totalGeneral = computed(() => {
    return this.subtotales().reduce((acc: number, sub: number) => acc + sub, 0);
  });

  // Signal Derivada: Saldo restante proyectado de la Caja Chica
  protected readonly saldoRestanteProyectado = computed(() => {
    const caja = this.cajaSeleccionada();
    if (!caja) return 0;
    return caja.saldoDisponible - this.totalGeneral();
  });

  // Alerta preventiva reactiva de techo presupuestal
  protected readonly alertaRnFin06 = computed(() => {
    const presupuestos = this.presupuestosCaja();
    if (!presupuestos || presupuestos.length === 0) return null;

    const lineas = this.lineasSignal();
    for (let i = 0; i < lineas.length; i++) {
      const l = lineas[i];
      if (l.centroCostoId) {
        const asig = presupuestos.find((p) => p.centroCostoId === Number(l.centroCostoId));
        const subtotal = (Number(l.cantidad) || 0) * (Number(l.precioUnitario) || 0);
        if (asig && subtotal > asig.saldoDisponible) {
          return `Aviso de Presupuesto: El importe en la línea #${i + 1} (S/ ${subtotal.toFixed(2)}) supera el disponible asignado a ${asig.nombreArea || asig.codigoCc} (S/ ${asig.saldoDisponible.toFixed(2)}).`;
        }
      }
    }
    return null;
  });

  // Selector Asistido y Verificación Instantánea de Presupuesto
  getCentroCostoLabel(cc: CentroCosto): string {
    const asig = this.presupuestosCaja().find((p) => p.centroCostoId === cc.id);
    if (asig) {
      return `${cc.nombreArea || cc.codigoCc} — Disponible: S/ ${asig.saldoDisponible.toFixed(2)}`;
    }
    return `${cc.nombreArea || cc.codigoCc}`;
  }

  getSaldoDisponibleCC(centroCostoId: any): number | null {
    if (!centroCostoId) return null;
    const asig = this.presupuestosCaja().find((p) => p.centroCostoId === Number(centroCostoId));
    return asig ? asig.saldoDisponible : null;
  }

  lineaSuperaPresupuesto(index: number): boolean {
    const lineas = this.lineasSignal();
    const l = lineas[index];
    if (!l || !l.centroCostoId) return false;
    const asig = this.presupuestosCaja().find((p) => p.centroCostoId === Number(l.centroCostoId));
    if (!asig) return false;
    const subtotal = (Number(l.cantidad) || 0) * (Number(l.precioUnitario) || 0);
    return subtotal > asig.saldoDisponible;
  }

  get detallesArray(): FormArray<FormGroup> {
    return this.form.get('detalles') as FormArray<FormGroup>;
  }

  ngOnInit(): void {
    this.cargarDatosIniciales();
    this.agregarLinea();

    this.detallesArray.valueChanges.subscribe(() => {
      this.actualizarLineasSignal();
    });

    this.form.get('cajaChicaId')?.valueChanges.subscribe((id) => {
      if (id && Number(id) > 0) {
        this.rendicionService.listarPresupuestosCaja(Number(id)).subscribe({
          next: (presupuestos: AsignacionCajaCentroCosto[]) => this.presupuestosCaja.set(presupuestos),
          error: (err: HttpErrorResponse) => this.presupuestosCaja.set([]),
        });
      }
    });

    const anio = new Date().getFullYear();
    const randomCorrelativo = Math.floor(100 + Math.random() * 900);
    this.form.patchValue({
      codigo: `RND-${anio}-${randomCorrelativo}`,
      responsable: 'Administración',
    });
  }

  private cargarDatosIniciales(): void {
    this.cargandoDatos.set(true);

    this.rendicionService.listarCajasChicas().subscribe({
      next: (cajas: CajaChica[]) => {
        this.cajasChicas.set(cajas);
        if (cajas.length > 0) {
          this.form.patchValue({ cajaChicaId: cajas[0].id });
          this.rendicionService.listarPresupuestosCaja(cajas[0].id).subscribe({
            next: (p: AsignacionCajaCentroCosto[]) => this.presupuestosCaja.set(p),
            error: (err: HttpErrorResponse) => this.presupuestosCaja.set([]),
          });
        }
        this.cargandoDatos.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorGeneral.set('No se pudieron cargar las Cajas Chicas desde el servidor.');
        this.cargandoDatos.set(false);
      },
    });

    this.rendicionService.listarCentrosCosto().subscribe({
      next: (ccs: CentroCosto[]) => this.centrosCosto.set(ccs),
      error: (e: HttpErrorResponse) => console.warn('No se pudieron precargar centros de costo:', e),
    });

    this.productoService.listar().subscribe({
      next: (prods: Producto[]) => this.productos.set(prods),
      error: (e: HttpErrorResponse) => console.warn('No se pudo precargar catálogo para autocomplete:', e),
    });
  }

  private crearGrupoLinea(
    concepto = '',
    cantidad = 1,
    precioUnitario = 0,
    productoId: number | null = null,
    centroCostoId: number | null = null
  ): FormGroup {
    return this.fb.group({
      concepto: [concepto, [Validators.required, Validators.maxLength(150)]],
      cantidad: [cantidad, [Validators.required, Validators.min(1)]],
      precioUnitario: [precioUnitario, [Validators.required, Validators.min(0.01)]],
      productoId: [productoId],
      centroCostoId: [centroCostoId, [Validators.required]],
    });
  }

  agregarLinea(): void {
    const primerCc = this.centrosCosto()[0]?.id ?? null;
    this.detallesArray.push(this.crearGrupoLinea('', 1, 0, null, primerCc));
    this.actualizarLineasSignal();
  }

  quitarLinea(index: number): void {
    if (this.detallesArray.length > 1) {
      this.detallesArray.removeAt(index);
      this.cerrarDropdown(index);
      this.actualizarLineasSignal();
    }
  }

  private actualizarLineasSignal(): void {
    const raw = this.detallesArray.getRawValue() as FormLineaItem[];
    this.lineasSignal.set(raw);
  }

  productosFiltrados(index: number): Producto[] {
    const filtro = (this.buscadorFiltro()[index] || '').toLowerCase().trim();
    if (!filtro) return this.productos().slice(0, 6);
    return this.productos()
      .filter((p) => p.nombre.toLowerCase().includes(filtro) || p.categoria?.nombre.toLowerCase().includes(filtro))
      .slice(0, 6);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.autocomplete-wrapper')) {
      this.dropdownAbierto.set(null);
    }
  }

  abrirDropdown(index: number): void {
    if (this.productosFiltrados(index).length > 0) {
      this.dropdownAbierto.set(index);
    }
  }

  cerrarDropdown(index: number): void {
    setTimeout(() => {
      if (this.dropdownAbierto() === index) {
        this.dropdownAbierto.set(null);
      }
    }, 250);
  }

  onInputConcepto(index: number, event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    const mapa = { ...this.buscadorFiltro(), [index]: query };
    this.buscadorFiltro.set(mapa);

    const matches = this.productosFiltrados(index);
    if (matches.length > 0) {
      this.dropdownAbierto.set(index);
    } else {
      this.dropdownAbierto.set(null);
    }
  }

  seleccionarProducto(index: number, prod: Producto): void {
    const grupo = this.detallesArray.at(index);
    if (grupo) {
      grupo.patchValue({
        concepto: prod.nombre,
        precioUnitario: prod.precio,
        productoId: prod.id,
      });
      grupo.markAsDirty();
      grupo.markAsTouched();
    }
    this.dropdownAbierto.set(null);
    this.actualizarLineasSignal();
  }

  esColeccionValida(): boolean {
    if (this.detallesArray.length < 1) return false;
    for (let i = 0; i < this.detallesArray.length; i++) {
      const g = this.detallesArray.at(i);
      if (g.invalid) return false;
    }
    return true;
  }

  abrirConfirmacion(): void {
    this.limpiarErrores();
    if (this.form.invalid || !this.esColeccionValida()) {
      this.form.markAllAsTouched();
      this.intentoEnvioInvalido.set(true);
      this.errorGeneral.set('Por favor complete todos los campos requeridos en la cabecera y en cada línea del detalle.');
      return;
    }
    this.intentoEnvioInvalido.set(false);
    this.mostrarModalConfirmacion.set(true);
  }

  cancelarConfirmacion(): void {
    this.mostrarModalConfirmacion.set(false);
  }

  confirmarYEnviar(): void {
    this.mostrarModalConfirmacion.set(false);
    this.limpiarErrores();
    this.loading.set(true);

    const raw = this.form.getRawValue();
    const payload: RendicionCajaChicaRequest = {
      codigo: raw.codigo.trim(),
      responsable: raw.responsable.trim(),
      cajaChicaId: Number(raw.cajaChicaId),
      detalles: raw.detalles.map((d: any) => ({
        concepto: d.concepto.trim(),
        cantidad: Number(d.cantidad),
        precioUnitario: Number(d.precioUnitario),
        productoId: d.productoId ? Number(d.productoId) : undefined,
        centroCostoId: d.centroCostoId ? Number(d.centroCostoId) : undefined,
      })),
    };

    this.rendicionService.registrarRendicion(payload).subscribe({
      next: (res: RendicionCajaChica) => {
        this.loading.set(false);
        this.mensajeExito.set(`¡Rendición ${res.codigo} registrada con éxito! Total procesado: S/ ${res.montoTotal.toFixed(2)}`);
        setTimeout(() => {
          this.router.navigate(['/finanzas/rendiciones']);
        }, 1500);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.manejarErrorServidor(err);
      },
    });
  }

  private manejarErrorServidor(err: HttpErrorResponse): void {
    const status = err.status;
    const body = err.error || {};
    const mensaje: string = body.message || '';
    const reglaNegocio: string = body.reglaNegocio || '';

    if (reglaNegocio === 'RN-FIN-06' || mensaje.includes('RN-FIN-06') || mensaje.toLowerCase().includes('presupuestal') || mensaje.toLowerCase().includes('centro de costo')) {
      this.errorSolvencia.set(
        `REGLA DE NEGOCIO RN-FIN-06 (Presupuesto Centro de Costo): ${mensaje}`
      );
      return;
    }

    if (reglaNegocio === 'RN-FIN-01' || mensaje.toLowerCase().includes('solvencia') || mensaje.toLowerCase().includes('supera la solvencia')) {
      this.errorSolvencia.set(
        `REGLA DE NEGOCIO RN-FIN-01 (Control de Liquidez): ${mensaje}`
      );
      return;
    }

    if (status === 400 && (mensaje.toLowerCase().includes('ya existe una rendición') || mensaje.toLowerCase().includes('código'))) {
      this.errorDuplicidad.set(
        `ERROR DE INTEGRIDAD: ${mensaje}. Por favor especifique un número correlativo único.`
      );
      return;
    }

    this.errorGeneral.set(
      mensaje || 'Ocurrió un error inesperado al procesar la rendición en el servidor.'
    );
  }

  private limpiarErrores(): void {
    this.errorSolvencia.set(null);
    this.errorDuplicidad.set(null);
    this.errorGeneral.set(null);
  }

  cancelar(): void {
    this.router.navigate(['/finanzas/rendiciones']);
  }
}
