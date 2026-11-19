import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { LoteService } from '../services/lote.service';
import { LoteResponse } from '../models/lote.model';
import { LoteFormDialogComponent, ProductoOpcion } from '../lote-form-dialog/lote-form-dialog.component';
import { ProductoService } from '../../productos/services/producto.service';

@Component({
    selector: 'app-lote-list',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatButtonModule,
        MatDialogModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatSelectModule,
        MatTableModule
    ],
    templateUrl: './lote-list.component.html',
    styleUrl: './lote-list.component.css'
})
export class LoteListComponent implements OnInit {
    private readonly loteService = inject(LoteService);
    private readonly productoService = inject(ProductoService);
    private readonly dialog = inject(MatDialog);

    readonly lotes = signal<LoteResponse[]>([]);
    readonly productos = signal<ProductoOpcion[]>([]);
    readonly busqueda = signal('');
    readonly productoFiltro = signal<number | null>(null);
    readonly columnas = ['numeroLote', 'productoNombre', 'cantidadActual', 'fechaProduccion', 'fechaVencimiento'];

    readonly lotesFiltrados = computed(() => {
        const q = this.busqueda().trim().toLowerCase();
        const lista = this.lotes();
        if (!q) return lista;
        return lista.filter(l =>
            l.numeroLote.toLowerCase().includes(q) ||
            l.productoNombre.toLowerCase().includes(q)
        );
    });

    ngOnInit(): void {
        this.cargarProductos();
        this.cargarLotes();
    }

    cargarProductos(): void {
        this.productoService.listar().subscribe({
            next: (res: any) => {
                const items = Array.isArray(res) ? res : (res?.content ?? []);
                this.productos.set(items.map((p: any) => ({
                    id: p.id,
                    nombre: p.nombre,
                    sku: p.sku,
                    unidadMedida: p.unidadMedida
                })));
            }
        });
    }

    cargarLotes(): void {
        this.loteService.listar(this.productoFiltro() ?? undefined).subscribe({
            next: (res) => this.lotes.set(res)
        });
    }

    cambiarFiltroProducto(productoId: number | null): void {
        this.productoFiltro.set(productoId);
        this.cargarLotes();
    }

    abrirModalIngreso(productoIdPreseleccionado?: number): void {
        const ref = this.dialog.open(LoteFormDialogComponent, {
            width: '560px',
            data: {
                productos: this.productos(),
                productoIdPreseleccionado: productoIdPreseleccionado ?? this.productoFiltro() ?? undefined
            }
        });

        ref.afterClosed().subscribe((creado) => {
            if (creado) {
                this.cargarLotes();
            }
        });
    }

    estaVencido(fechaVencimiento: string): boolean {
        if (!fechaVencimiento) return false;
        const hoy = new Date().toISOString().split('T')[0];
        return fechaVencimiento < hoy;
    }
}