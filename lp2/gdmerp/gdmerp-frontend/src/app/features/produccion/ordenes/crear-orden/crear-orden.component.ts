import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion, OrdenProduccionRequest } from '../../models/produccion.models';

@Component({
  selector: 'app-crear-orden',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div style="padding: 1.5rem; max-width: 900px; margin: 0 auto;">
      <div style="margin-bottom: 1.5rem;">
        <a routerLink="/produccion/ordenes" style="color: #d97706; text-decoration: none; font-size: 0.875rem; font-weight: 500; display: inline-flex; align-items: center; gap: 0.25rem;">
          ← Volver al listado de órdenes
        </a>
        <h2 style="font-size: 1.75rem; font-weight: 700; color: #111827; margin: 0.5rem 0 0.25rem 0;">
          Nueva Orden de Producción
        </h2>
      </div>

      <div style="background: white; border-radius: 0.75rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e5e7eb; padding: 2rem;">
        <form [formGroup]="form" (ngSubmit)="guardar()">
          
          <h3 style="font-size: 1.1rem; font-weight: 600; color: #374151; margin-top: 0; margin-bottom: 1.25rem; border-bottom: 1px solid #f3f4f6; padding-bottom: 0.5rem;">
            1. Parámetros de la Orden
          </h3>

          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.25rem; margin-bottom: 1.5rem;">
            <div>
              <label style="display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem;">
                Producto Terminado *
              </label>
              <select formControlName="productoTerminadoId" style="width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.9rem; background-color: white;">
                <option [value]="3">Aceite de Coco Virgen Extra 500ml</option>
              </select>
            </div>

            <div>
              <label style="display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem;">
                Cantidad Planificada *
              </label>
              <input type="number" formControlName="cantidadPlanificada" min="1" style="width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.9rem; box-sizing: border-box;">
            </div>

            <div>
              <label style="display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem;">
                Centro de Trabajo *
              </label>
              <select formControlName="centroTrabajoId" style="width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.9rem; background-color: white;">
                <option [value]="1">Línea de Prensa Fría y Filtrado</option>
              </select>
            </div>

            <div>
              <label style="display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem;">
                Estado Inicial *
              </label>
              <select formControlName="estado" style="width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.9rem; background-color: white;">
                @for (est of estados; track est) {
                  <option [value]="est">{{ est }}</option>
                }
              </select>
            </div>

            <div>
              <label style="display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem;">
                Fecha Planificada Inicio *
              </label>
              <input type="date" formControlName="fechaPlanificadaInicio" style="width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.9rem; box-sizing: border-box;">
            </div>

            <div>
              <label style="display: block; font-size: 0.875rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem;">
                Fecha Estimada Fin *
              </label>
              <input type="date" formControlName="fechaEstimadaFin" style="width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.9rem; box-sizing: border-box;">
            </div>
          </div>

          <div style="margin-top: 2rem; margin-bottom: 2rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f3f4f6; padding-bottom: 0.5rem; margin-bottom: 1rem;">
              <h3 style="font-size: 1.1rem; font-weight: 600; color: #374151; margin: 0;">
                2. Insumos y Materias Primas Requeridas
              </h3>
              <button type="button" (click)="agregarMateriaPrima()" style="background: #fef3c7; color: #b45309; border: 1px solid #fcd34d; padding: 0.375rem 0.75rem; border-radius: 0.375rem; font-size: 0.85rem; font-weight: 600; cursor: pointer;">
                + Agregar Insumo
              </button>
            </div>

            <div formArrayName="detalles" style="display: flex; flex-direction: column; gap: 0.75rem;">
              @for (detalle of detalles.controls; track $index; let i = $index) {
                <div [formGroupName]="i" style="display: flex; align-items: center; gap: 1rem; background-color: #f9fafb; padding: 0.875rem; border-radius: 0.5rem; border: 1px solid #e5e7eb;">
                  <div style="font-weight: 700; color: #6b7280; font-size: 0.85rem; width: 25px;">#{{ i + 1 }}</div>
                  
                  <div style="flex: 2;">
                    <label style="display: block; font-size: 0.75rem; color: #4b5563; font-weight: 600; margin-bottom: 0.25rem;">Materia Prima</label>
                    <select formControlName="materiaPrimaId" style="width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.875rem; background: white;">
                      <option [value]="1">Coco Fresco Entero (Con cáscara)</option>
                      <option [value]="2">Botella de Vidrio Ámbar 500ml</option>
                    </select>
                  </div>

                  <div style="flex: 1;">
                    <label style="display: block; font-size: 0.75rem; color: #4b5563; font-weight: 600; margin-bottom: 0.25rem;">Cantidad</label>
                    <input type="number" formControlName="cantidadRequerida" min="0.01" step="0.01" style="width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.875rem; box-sizing: border-box;">
                  </div>

                  <div style="padding-top: 1.1rem;">
                    <button type="button" (click)="eliminarMateriaPrima(i)" [disabled]="detalles.length === 1" style="background: transparent; color: #ef4444; border: none; font-size: 1.25rem; cursor: pointer; padding: 0.25rem 0.5rem;">✕</button>
                  </div>
                </div>
              }
            </div>
          </div>

          @if (errorMessage) {
            <div style="background-color: #fee2e2; border: 1px solid #f87171; color: #b91c1c; padding: 0.75rem 1rem; border-radius: 0.375rem; font-size: 0.875rem; margin-bottom: 1.5rem;">
              {{ errorMessage }}
            </div>
          }

          <div style="display: flex; justify-content: flex-end; gap: 1rem; border-top: 1px solid #f3f4f6; padding-top: 1.5rem;">
            <a routerLink="/produccion/ordenes" style="padding: 0.625rem 1.25rem; border-radius: 0.375rem; border: 1px solid #d1d5db; background: white; color: #374151; font-weight: 600; text-decoration: none; font-size: 0.95rem;">
              Cancelar
            </a>
            <button type="submit" [disabled]="form.invalid || isSubmitting" [style.opacity]="form.invalid || isSubmitting ? '0.6' : '1'" style="background-color: #16a34a; color: white; border: none; padding: 0.625rem 1.5rem; border-radius: 0.375rem; font-weight: 600; font-size: 0.95rem; cursor: pointer;">
              @if (isSubmitting) { <span>Guardando...</span> } @else { <span>Guardar Orden</span> }
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class CrearOrdenComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ordenService = inject(OrdenProduccionService);
  private readonly router = inject(Router);

  readonly estados = Object.values(EstadoOrdenProduccion);
  isSubmitting = false;
  errorMessage = '';

  readonly form: FormGroup = this.fb.group({
    productoTerminadoId: [3, [Validators.required, Validators.min(1)]],
    cantidadPlanificada: [100, [Validators.required, Validators.min(1)]],
    estado: [EstadoOrdenProduccion.PLANIFICADO, [Validators.required]],
    centroTrabajoId: [1, [Validators.required, Validators.min(1)]],
    fechaPlanificadaInicio: [this.getTodayDateString(), [Validators.required]],
    fechaEstimadaFin: [this.getDefaultEndDateString(), [Validators.required]],
    detalles: this.fb.array([])
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  ngOnInit(): void {
    this.agregarMateriaPrima(1, 500); 
    this.agregarMateriaPrima(2, 100); 
  }

  agregarMateriaPrima(materiaPrimaId: number = 1, cantidadRequerida: number = 10): void {
    const detalleGroup = this.fb.group({
      materiaPrimaId: [materiaPrimaId, [Validators.required, Validators.min(1)]],
      cantidadRequerida: [cantidadRequerida, [Validators.required, Validators.min(0.01)]]
    });
    this.detalles.push(detalleGroup);
  }

  eliminarMateriaPrima(index: number): void {
    this.detalles.removeAt(index);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
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
      detalles: (raw.detalles || []).map((d: { materiaPrimaId: number | string; cantidadRequerida: number | string }) => ({
        materiaPrimaId: Number(d.materiaPrimaId),
        cantidadRequerida: Number(d.cantidadRequerida)
      }))
    };

    this.ordenService.crear(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigate(['/produccion/ordenes']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = 'Error al registrar la orden. Verifica la consola o el backend.';
        console.error(err);
      }
    });
  }

  private formatFecha(fecha: string): string {
    if (!fecha) return '';
    if (fecha.includes('T')) return fecha;
    return `${fecha}T08:00:00`;
  }

  private getTodayDateString(): string {
    return new Date().toISOString().split('T')[0];
  }

  private getDefaultEndDateString(): string {
    const date = new Date();
    date.setDate(date.getDate() + 7);
    return date.toISOString().split('T')[0];
  }
}