import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { toSignal } from '@angular/core/rxjs-interop';
import { CompraService } from '../services/compra.service';
import { CompraRequest, Insumo, InsumoCatalogo } from '../models/compra.model';
import { ProveedorService } from '../../proveedor/services/proveedor.service';
import { Proveedor } from '../../proveedor/models/proveedor.model';

@Component({
  selector: 'app-compra-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CurrencyPipe, MatSnackBarModule, MatIconModule],
  templateUrl: './compra-form.component.html',
  styleUrl: './compra-form.component.css',
})
export class CompraForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly compraService = inject(CompraService);
  private readonly proveedorService = inject(ProveedorService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly proveedores = signal<Proveedor[]>([]);
  protected readonly catalogoInsumos = signal<InsumoCatalogo[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly modalConfirmacion = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    proveedorId: [0, [Validators.required, Validators.min(1)]],
    serieComprobante: ['', [Validators.required, Validators.maxLength(10)]],
    numeroComprobante: ['', [Validators.required, Validators.maxLength(30)]],
    fechaEmision: [new Date().toISOString().slice(0, 10), [Validators.required]],
    condicionPago: ['CONTADO', [Validators.required]],
    costoFlete: [0, [Validators.min(0)]],
    detalles: this.fb.array([this.crearLineaVacia()], [Validators.minLength(1)]),
  });

  protected get detallesArray() {
    return this.form.controls.detalles;
  }

  protected readonly valoresDetalles = toSignal(this.form.controls.detalles.valueChanges, {
    initialValue: this.form.controls.detalles.getRawValue(),
  });

  protected readonly subtotalesLinea = computed(() =>
    this.valoresDetalles().map((d) => (Number(d.cantidad) || 0) * (Number(d.precioUnitario) || 0))
  );

  protected readonly cantidadesIngreso = computed(() =>
    this.valoresDetalles().map((d) => (Number(d.cantidad) || 0) * (Number(d.factorConversion) || 1))
  );

  protected readonly subtotalNeto = computed(() =>
    this.subtotalesLinea().reduce((acc: number, s: number) => acc + s, 0)
  );

  protected readonly igvCalculado = computed(() =>
    Math.round(this.subtotalNeto() * 0.18 * 100) / 100
  );

  protected readonly fleteVal = computed(() =>
    Number(this.form.controls.costoFlete.value) || 0
  );

  protected readonly totalFactura = computed(() =>
    this.subtotalNeto() + this.igvCalculado() + this.fleteVal()
  );

  ngOnInit(): void {
    // 1. Cargar directorio de proveedores
    this.proveedorService.listarProveedores().subscribe({
      next: (provs: Proveedor[]) => {
        this.proveedores.set(provs);
        if (provs.length > 0) {
          this.form.patchValue({ proveedorId: provs[0].id });
        }
      },
      error: () => {
        this.snackBar.open('❌ No se pudo cargar el directorio de proveedores.', 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar'],
        });
      },
    });

    // 2. Cargar catálogo de insumos dinámicos reales desde backend
    this.compraService.listarInsumos().subscribe({
      next: (insumosApi: Insumo[]) => {
        if (insumosApi && insumosApi.length > 0) {
          const mapeados: InsumoCatalogo[] = insumosApi.map((i: Insumo) => ({
            id: i.id,
            nombre: `${i.nombre} (${i.unidadMedida})`,
            unidad: i.unidadMedida,
            precioSugerido: Number(i.precioSugerido) || 10.0,
          }));
          this.catalogoInsumos.set(mapeados);

          // Inicializar la primera línea con el primer insumo real
          if (this.detallesArray.length > 0) {
            const primer = mapeados[0];
            this.detallesArray.at(0).patchValue({
              insumoId: primer.id,
              nombreInsumo: primer.nombre,
              precioUnitario: primer.precioSugerido,
            });
          }
        }
      },
      error: () => {
        this.snackBar.open('❌ No se pudo cargar el catálogo de insumos.', 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  private crearLineaVacia() {
    const hoy = new Date();
    const vencimientoDefault = new Date(hoy.setFullYear(hoy.getFullYear() + 1)).toISOString().slice(0, 10);

    return this.fb.nonNullable.group({
      insumoId: [0, [Validators.required, Validators.min(1)]],
      nombreInsumo: ['', [Validators.required, Validators.maxLength(150)]],
      loteProveedor: [''],
      fechaVencimiento: [vencimientoDefault],
      factorConversion: [1, [Validators.required, Validators.min(0.01)]],
      cantidad: [1, [Validators.required, Validators.min(0.01)]],
      precioUnitario: [0, [Validators.required, Validators.min(0)]],
    });
  }

  agregarLinea(): void {
    const catalogo = this.catalogoInsumos();
    const sugerido = catalogo.length > 0
      ? catalogo[Math.min(this.detallesArray.length, catalogo.length - 1)]
      : { id: 0, nombre: '', unidad: 'UND', precioSugerido: 10 };

    const hoy = new Date();
    const vencimientoDefault = new Date(hoy.setFullYear(hoy.getFullYear() + 1)).toISOString().slice(0, 10);

    this.detallesArray.push(
      this.fb.nonNullable.group({
        insumoId: [sugerido.id, [Validators.required, Validators.min(1)]],
        nombreInsumo: [sugerido.nombre, [Validators.required, Validators.maxLength(150)]],
        loteProveedor: [''],
        fechaVencimiento: [vencimientoDefault],
        factorConversion: [1, [Validators.required, Validators.min(0.01)]],
        cantidad: [50, [Validators.required, Validators.min(0.01)]],
        precioUnitario: [sugerido.precioSugerido, [Validators.required, Validators.min(0.01)]],
      })
    );
  }

  eliminarLinea(index: number): void {
    if (this.detallesArray.length > 1) {
      this.detallesArray.removeAt(index);
    } else {
      this.snackBar.open('⚠️ La orden debe tener al menos un ítem de insumo.', 'Cerrar', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top',
        panelClass: ['warning-snackbar'],
      });
    }
  }

  seleccionarInsumoCatalogo(index: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const idSeleccionado = Number(select.value);
    const encontrado = this.catalogoInsumos().find((i: InsumoCatalogo) => i.id === idSeleccionado);
    if (encontrado) {
      this.detallesArray.at(index).patchValue({
        insumoId: encontrado.id,
        nombreInsumo: encontrado.nombre,
        precioUnitario: encontrado.precioSugerido,
      });
    }
  }

  abrirConfirmacion(): void {
    if (this.form.invalid || this.detallesArray.length === 0) {
      this.form.markAllAsTouched();
      this.snackBar.open('⚠️ Por favor verifique los campos requeridos en el formulario de compra.', 'Cerrar', {
        duration: 4000,
        horizontalPosition: 'right',
        verticalPosition: 'top',
        panelClass: ['warning-snackbar'],
      });
      return;
    }
    this.error.set(null);
    this.modalConfirmacion.set(true);
  }

  cerrarConfirmacion(): void {
    this.modalConfirmacion.set(false);
  }

  proveedorSeleccionado(): Proveedor | undefined {
    const id = Number(this.form.controls.proveedorId.value);
    return this.proveedores().find((p: Proveedor) => p.id === id);
  }

  guardar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.modalConfirmacion.set(false);

    const raw = this.form.getRawValue();
    const serie = raw.serieComprobante.trim().toUpperCase();
    const numero = raw.numeroComprobante.trim();
    const numeroCompleto = `${serie}-${numero}`;

    const request: CompraRequest = {
      proveedorId: Number(raw.proveedorId),
      serieComprobante: serie,
      numeroComprobante: numeroCompleto,
      fechaEmision: raw.fechaEmision,
      condicionPago: raw.condicionPago,
      costoFlete: Number(raw.costoFlete) || 0,
      detalles: raw.detalles.map((d) => ({
        insumoId: Number(d.insumoId),
        nombreInsumo: d.nombreInsumo.trim(),
        loteProveedor: d.loteProveedor ? d.loteProveedor.trim().toUpperCase() : undefined,
        fechaVencimiento: d.fechaVencimiento || undefined,
        factorConversion: Number(d.factorConversion) || 1,
        cantidad: Number(d.cantidad),
        precioUnitario: Number(d.precioUnitario),
      })),
    };

    this.compraService.crearCompra(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open(`✅ Orden de compra ${numeroCompleto} registrada exitosamente.`, 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar'],
        });
        this.router.navigate(['/compras']);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message;
        const errMsg = msg || 'Error al registrar la orden de compra.';
        this.error.set(errMsg);
        this.loading.set(false);
        this.snackBar.open(`❌ ${errMsg}`, 'Entendido', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar'],
        });
      },
    });
  }
}
