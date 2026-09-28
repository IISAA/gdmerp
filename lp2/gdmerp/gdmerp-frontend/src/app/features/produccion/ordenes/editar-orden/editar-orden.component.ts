import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormArray, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { OrdenProduccionService } from '../../services/orden-produccion.service';
import { EstadoOrdenProduccion, OrdenProduccionRequest } from '../../models/produccion.models';

@Component({
  selector: 'app-editar-orden',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div style="padding: 1.5rem; max-width: 900px; margin: 0 auto;">
      <h2 style="font-size: 1.75rem; font-weight: 700; margin-bottom: 1.5rem;">Editar Orden #{{ ordenId }}</h2>
      <div style="background: white; padding: 2rem; border-radius: 0.75rem; border: 1px solid #e5e7eb;">
        <form [formGroup]="form" (ngSubmit)="actualizar()">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem;">
            <div>
              <label>Estado</label>
              <select formControlName="estado" style="width: 100%; padding: 0.5rem;">
                @for (est of estados; track est) { <option [value]="est">{{ est }}</option> }
              </select>
            </div>
            <div>
              <label>Cantidad Planificada</label>
              <input type="number" formControlName="cantidadPlanificada" style="width: 100%; padding: 0.5rem;">
            </div>
          </div>
          
          <div style="display: flex; justify-content: flex-end; gap: 1rem;">
            <a routerLink="/produccion/ordenes" style="padding: 0.5rem 1rem; border: 1px solid #ccc; text-decoration: none; color: black;">Cancelar</a>
            <button type="submit" [disabled]="form.invalid" style="background: #3b82f6; color: white; padding: 0.5rem 1rem; border: none; cursor: pointer;">
              Actualizar Orden
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class EditarOrdenComponent implements OnInit {
  private fb = inject(FormBuilder);
  private ordenService = inject(OrdenProduccionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  estados = Object.values(EstadoOrdenProduccion);
  ordenId!: number;
  
  form: FormGroup = this.fb.group({
    productoTerminadoId: [3, Validators.required],
    cantidadPlanificada: [100, Validators.required],
    estado: [EstadoOrdenProduccion.PLANIFICADO, Validators.required],
    centroTrabajoId: [1, Validators.required],
    fechaPlanificadaInicio: ['', Validators.required],
    fechaEstimadaFin: ['', Validators.required],
    detalles: this.fb.array([])
  });

  ngOnInit() {
    this.ordenId = Number(this.route.snapshot.paramMap.get('id'));
    this.ordenService.obtenerPorId(this.ordenId).subscribe(orden => {
      // Llenamos el formulario con los datos que vienen del backend
      this.form.patchValue({
        cantidadPlanificada: orden.cantidadPlanificada,
        estado: orden.estado,
        fechaPlanificadaInicio: orden.fechaPlanificadaInicio.split('T')[0],
        fechaEstimadaFin: orden.fechaEstimadaFin.split('T')[0]
      });
    });
  }

  actualizar() {
    if (this.form.invalid) return;
    
    const request = this.form.getRawValue();
    // Aseguramos el formato de fecha para Spring Boot
    request.fechaPlanificadaInicio += 'T08:00:00';
    request.fechaEstimadaFin += 'T08:00:00';

    this.ordenService.actualizar(this.ordenId, request).subscribe(() => {
      this.router.navigate(['/produccion/ordenes']);
    });
  }
}