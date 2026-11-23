import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MovimientoInventarioService } from '../services/movimiento-inventario.service';
import { TipoMovimientoManual } from '../models/movimiento-inventario.model';

export interface MovimientoDialogData {
    productos: { id: number; sku: string; nombre: string; stockTotal?: number; costoPromedio?: number }[];
    productoIdPreseleccionado?: number | null;
}

@Component({
    selector: 'app-movimiento-form-dialog',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatSnackBarModule,
    ],
    templateUrl: './movimiento-form-dialog.component.html',
    styleUrl: './movimiento-form-dialog.component.scss',
})
export class MovimientoFormDialogComponent {
    private readonly fb = inject(FormBuilder);
    private readonly movimientoService = inject(MovimientoInventarioService);
    private readonly snackBar = inject(MatSnackBar);

    readonly dialogRef = inject(MatDialogRef<MovimientoFormDialogComponent>);
    readonly data = inject<MovimientoDialogData>(MAT_DIALOG_DATA);

    guardando = false;

    readonly tiposMovimiento: { valor: TipoMovimientoManual; etiqueta: string }[] = [
        { valor: 'ENTRADA', etiqueta: 'Entrada (Compra / Ingreso)' },
        { valor: 'SALIDA', etiqueta: 'Salida (Despacho / Merma)' },
        { valor: 'AJUSTE_POSITIVO', etiqueta: 'Ajuste Positivo (+)' },
        { valor: 'AJUSTE_NEGATIVO', etiqueta: 'Ajuste Negativo (-)' },
    ];

    form = this.fb.group({
        productoId: [this.data.productoIdPreseleccionado ?? null as number | null, Validators.required],
        tipoMovimiento: ['ENTRADA' as TipoMovimientoManual, Validators.required],
        cantidad: [null as number | null, [Validators.required, Validators.min(0.0001)]],
        costoUnitario: [null as number | null, [Validators.min(0)]],
        referencia: ['', [Validators.required, Validators.maxLength(120)]],
    });

    guardar(): void {
        if (this.form.invalid) return;
        this.guardando = true;

        const raw = this.form.getRawValue();
        this.movimientoService.registrarMovimiento({
            productoId: raw.productoId!,
            tipoMovimiento: raw.tipoMovimiento!,
            cantidad: Number(raw.cantidad),
            costoUnitario: raw.costoUnitario !== null ? Number(raw.costoUnitario) : null,
            referencia: raw.referencia!.trim(),
        }).subscribe({
            next: () => {
                this.snackBar.open('Movimiento registrado en el Kardex', 'OK', { duration: 3000 });
                this.dialogRef.close(true);
            },
            error: (err) => {
                this.guardando = false;
                const msg = err?.error?.message || err?.error?.detail || 'No se pudo registrar el movimiento';
                this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
            },
        });
    }
}