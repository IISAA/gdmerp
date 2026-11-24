import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LoteRequest } from '../models/lote.model';
import { LoteService } from '../services/lote.service';

export interface ProductoOpcion {
    id: number;
    nombre: string;
    sku?: string;
    unidadMedida?: string;
}

export interface LoteFormDialogData {
    productos: ProductoOpcion[];
    productoIdPreseleccionado?: number;
}

@Component({
    selector: 'app-lote-form-dialog',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatSnackBarModule,
        MatIconModule,
        MatDatepickerModule
    ],
    templateUrl: './lote-form-dialog.component.html',
    styleUrl: './lote-form-dialog.component.css'
})
export class LoteFormDialogComponent implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly loteService = inject(LoteService);
    private readonly snackBar = inject(MatSnackBar);
    private readonly dialogRef = inject(MatDialogRef<LoteFormDialogComponent>);
    readonly data = inject<LoteFormDialogData>(MAT_DIALOG_DATA);

    readonly guardando = signal(false);
    readonly cargandoCodigo = signal(false);

    readonly form = this.fb.group({
        productoId: [null as number | null, Validators.required],
        numeroLote: ['', [Validators.required, Validators.maxLength(50)]],
        cantidadActual: [null as number | null, [Validators.required, Validators.min(0.0001)]],
        fechaProduccion: [new Date() as Date | null, Validators.required],
        fechaVencimiento: [null as Date | null, Validators.required]
    });

    ngOnInit(): void {
        if (this.data.productoIdPreseleccionado != null) {
            this.form.patchValue({ productoId: this.data.productoIdPreseleccionado });
        }
        this.cargarSiguienteCodigo();
    }

    cargarSiguienteCodigo(): void {
        this.cargandoCodigo.set(true);
        this.loteService.obtenerSiguienteCodigo().subscribe({
            next: ({ codigo }) => {
                if (!this.form.controls.numeroLote.dirty) {
                    this.form.patchValue({ numeroLote: codigo });
                }
                this.cargandoCodigo.set(false);
            },
            error: () => {
                this.cargandoCodigo.set(false);
            }
        });
    }

    regenerarCodigo(): void {
        this.form.controls.numeroLote.markAsPristine();
        this.cargarSiguienteCodigo();
    }

    guardar(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const raw = this.form.getRawValue();
        if (raw.productoId == null || raw.cantidadActual == null) return;

        const fechaProduccionStr = this.formatearLocalDate(raw.fechaProduccion);
        const fechaVencimientoStr = this.formatearLocalDate(raw.fechaVencimiento);

        if (fechaVencimientoStr < fechaProduccionStr) {
            this.snackBar.open(
                'La fecha de vencimiento no puede ser anterior a la de producción',
                'Cerrar',
                { duration: 3500 }
            );
            return;
        }

        this.guardando.set(true);
        const payload: LoteRequest = {
            productoId: raw.productoId,
            numeroLote: (raw.numeroLote ?? '').trim(),
            cantidadActual: Number(raw.cantidadActual),
            fechaProduccion: fechaProduccionStr,
            fechaVencimiento: fechaVencimientoStr
        };

        this.loteService.registrarIngreso(payload).subscribe({
            next: (creado) => {
                this.guardando.set(false);
                this.snackBar.open(`Lote ${creado.numeroLote} registrado y stock actualizado`, 'OK', { duration: 3000 });
                this.dialogRef.close(creado);
            },
            error: (err) => {
                this.guardando.set(false);
                const msg = err?.error?.message || 'No se pudo registrar el lote';
                this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
            }
        });
    }

    cancelar(): void {
        this.dialogRef.close(null);
    }

    private formatearLocalDate(fecha: Date | string | null): string {
        if (!fecha) return '';
        if (typeof fecha === 'string') return fecha.split('T')[0];

        const anio = fecha.getFullYear();
        const mes = String(fecha.getMonth() + 1).padStart(2, '0');
        const dia = String(fecha.getDate()).padStart(2, '0');
        return `${anio}-${mes}-${dia}`;
    }
}