import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { ProductoService } from '../services/producto.service';
import { ProductoResponse } from '../models/producto.model';

@Component({
  selector: 'app-lista-productos',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    RouterLink,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatChipsModule
  ],
  templateUrl: './lista-productos.component.html',
  styleUrls: ['./lista-productos.component.css']
})
export class ListaProductosComponent implements OnInit {
  private readonly productoService = inject(ProductoService);
  private readonly snackBar = inject(MatSnackBar);

  // Estado principal
  readonly productos = signal<ProductoResponse[]>([]);
  readonly textoBusqueda = signal<string>('');
  readonly filtroTipo = signal<string>('TODOS');
  readonly isLoading = signal<boolean>(false);

  // Métricas KPI
  readonly totalProductos = computed(() => this.productos().length);
  readonly totalMateriasPrimas = computed(() =>
    this.productos().filter(p => p.tipoProducto === 'MATERIA_PRIMA').length
  );
  readonly totalTerminados = computed(() =>
    this.productos().filter(p => p.tipoProducto === 'TERMINADO').length
  );
  readonly totalCriticos = computed(() =>
    this.productos().filter(p => (p.stockTotal ?? p.stock ?? 0) <= (p.stockMinimo ?? 0)).length
  );

  // Filtro Reactivo
  readonly productosFiltrados = computed(() => {
    let lista = this.productos() || [];
    const tipo = this.filtroTipo();

    if (tipo === 'CRITICO') {
      lista = lista.filter(p => (p.stockTotal ?? p.stock ?? 0) <= (p.stockMinimo ?? 0));
    } else if (tipo !== 'TODOS') {
      lista = lista.filter(p => p.tipoProducto === tipo);
    }

    const termino = this.textoBusqueda().toLowerCase().trim();
    if (!termino) return lista;

    return lista.filter(p =>
      (p.nombre && p.nombre.toLowerCase().includes(termino)) ||
      (p.sku && p.sku.toLowerCase().includes(termino)) ||
      (p.tipoProducto && p.tipoProducto.toLowerCase().includes(termino)) ||
      (p.categoria?.nombre && p.categoria.nombre.toLowerCase().includes(termino)) ||
      (p.almacen?.nombre && p.almacen.nombre.toLowerCase().includes(termino))
    );
  });

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(mostrarNotificacion = false): void {
    this.isLoading.set(true);
    this.productoService.listar().subscribe({
      next: (data) => {
        const lista = Array.isArray(data) ? data : ((data as any)?.content || []);
        this.productos.set(lista);
        this.isLoading.set(false);
        if (mostrarNotificacion) {
          this.snackBar.open('🔄 Catálogo de productos actualizado.', 'Cerrar', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['info-snackbar']
          });
        }
      },
      error: (err) => {
        console.error('Error cargando catálogo de productos:', err);
        this.productos.set([]);
        this.isLoading.set(false);
        this.snackBar.open('❌ No se pudo cargar el catálogo de productos.', 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onBuscar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
  }

  filtrarPor(tipo: string): void {
    this.filtroTipo.set(tipo);
  }

  eliminar(id: number, nombre: string): void {
    if (!confirm(`¿Estás seguro de eliminar el producto "${nombre}"?\nNota: Esta acción solo procede si el producto no tiene movimientos de stock ni órdenes asociadas.`)) {
      return;
    }

    this.productoService.eliminar(id).subscribe({
      next: () => {
        this.productos.update(actuales => actuales.filter(p => p.id !== id));
        this.snackBar.open(`✅ Producto "${nombre}" eliminado del catálogo.`, 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        console.error(err);
        const mensajeBackend = err.error?.message;
        const msg = mensajeBackend
          ? `❌ Error: ${mensajeBackend}`
          : '❌ No se pudo eliminar el producto (Restricción de integridad o histórico existente).';
        this.snackBar.open(msg, 'Entendido', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }
}

export { ListaProductosComponent as ProductoListComponent };
