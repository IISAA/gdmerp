import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';

import { MovimientoInventarioService } from '../services/movimiento-inventario.service';
import { KardexFiltro, MovimientoInventarioResponse } from '../models/movimiento-inventario.model';
import { MovimientoFormDialogComponent } from '../movimiento-form-dialog/movimiento-form-dialog.component';
import { ProductoService } from '../../productos/services/producto.service';

@Component({
    selector: 'app-kardex-list',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        DatePipe,
        DecimalPipe,
        MatTableModule,
        MatPaginatorModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatCardModule,
        MatDialogModule,
        MatProgressBarModule,
        MatSnackBarModule,
        MatDatepickerModule,
    ],
    templateUrl: './kardex-list.component.html',
    styleUrl: './kardex-list.component.scss',
})
export class KardexListComponent implements OnInit {
    private readonly movimientoService = inject(MovimientoInventarioService);
    private readonly productoService = inject(ProductoService);
    private readonly dialog = inject(MatDialog);
    private readonly snackBar = inject(MatSnackBar);
    private readonly route = inject(ActivatedRoute);

    displayedColumns: string[] = [
        'fechaMovimiento',
        'producto',
        'tipoMovimiento',
        'cantidad',
        'stockAnterior',
        'stockResultante',
        'costoUnitario',
        'referencia',
    ];

    dataSource = new MatTableDataSource<MovimientoInventarioResponse>([]);
    productos: any[] = [];
    cargando = false;

    // Fechas enlazadas a los MatDatepicker
    fechaDesde: Date | null = null;
    fechaHasta: Date | null = null;

    filtro: KardexFiltro = {
        productoId: null,
        tipoMovimiento: null,
        desde: null,
        hasta: null,
    };

    readonly tiposFiltro = [
        { valor: 'ENTRADA', etiqueta: 'Entrada Manual' },
        { valor: 'ENTRADA_LOTE', etiqueta: 'Ingreso de Lote' },
        { valor: 'ENTRADA_PRODUCCION', etiqueta: 'Ingreso Producción' },
        { valor: 'DEVOLUCION_PRODUCCION', etiqueta: 'Devolución Producción' },
        { valor: 'SALIDA', etiqueta: 'Salida Manual' },
        { valor: 'SALIDA_PRODUCCION', etiqueta: 'Consumo Producción (FEFO)' },
        { valor: 'AJUSTE_POSITIVO', etiqueta: 'Ajuste Positivo' },
        { valor: 'AJUSTE_NEGATIVO', etiqueta: 'Ajuste Negativo' },
    ];

    @ViewChild(MatPaginator) set paginator(p: MatPaginator) {
        if (p) this.dataSource.paginator = p;
    }

    ngOnInit(): void {
        const queryProdId = this.route.snapshot.queryParamMap.get('productoId');
        if (queryProdId) {
            this.filtro.productoId = Number(queryProdId);
        }

        this.cargarProductos();
        this.cargarKardex();
    }

    cargarProductos(): void {
        this.productoService.listar().subscribe({
            next: (res: any[]) => (this.productos = res),
            error: () => this.snackBar.open('No se pudo cargar la lista de productos', 'Cerrar', { duration: 3000 }),
        });
    }

    cargarKardex(): void {
        this.cargando = true;
        this.filtro.desde = this.formatearFechaIso(this.fechaDesde);
        this.filtro.hasta = this.formatearFechaIso(this.fechaHasta);

        this.movimientoService.listarKardex(this.filtro).subscribe({
            next: (datos) => {
                this.dataSource.data = datos;
                this.cargando = false;
            },
            error: () => {
                this.cargando = false;
                this.snackBar.open('Error al cargar el historial de Kardex', 'Cerrar', { duration: 4000 });
            },
        });
    }

    limpiarFiltros(): void {
        this.fechaDesde = null;
        this.fechaHasta = null;
        this.filtro = { productoId: null, tipoMovimiento: null, desde: null, hasta: null };
        this.cargarKardex();
    }

    abrirNuevoMovimiento(): void {
        const ref = this.dialog.open(MovimientoFormDialogComponent, {
            width: '520px',
            data: {
                productos: this.productos,
                productoIdPreseleccionado: this.filtro.productoId,
            },
        });

        ref.afterClosed().subscribe((registrado) => {
            if (registrado) {
                this.cargarProductos();
                this.cargarKardex();
            }
        });
    }

    esIngreso(tipo: string): boolean {
        return ['ENTRADA', 'ENTRADA_LOTE', 'ENTRADA_PRODUCCION', 'DEVOLUCION_PRODUCCION', 'AJUSTE_POSITIVO'].includes(tipo);
    }

    private formatearFechaIso(fecha: Date | null): string | null {
        if (!fecha) return null;
        const anio = fecha.getFullYear();
        const mes = String(fecha.getMonth() + 1).padStart(2, '0');
        const dia = String(fecha.getDate()).padStart(2, '0');
        return `${anio}-${mes}-${dia}`;
    }
}