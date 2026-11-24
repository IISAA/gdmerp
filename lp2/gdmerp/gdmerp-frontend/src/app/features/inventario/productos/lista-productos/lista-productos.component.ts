import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
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
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './lista-productos.component.html',
  styleUrls: ['./lista-productos.component.css']
})
export class ListaProductosComponent implements OnInit {
  private readonly productoService = inject(ProductoService);
  private readonly snackBar = inject(MatSnackBar);

  readonly columnas = ['sku', 'producto', 'categoria', 'almacen', 'stock', 'acciones'];

  protected readonly productos = signal<ProductoResponse[]>([]);
  protected readonly textoBusqueda = signal<string>('');
  protected readonly isLoading = signal<boolean>(false);

  protected readonly productosFiltrados = computed(() => {
    const termino = this.textoBusqueda().toLowerCase().trim();
    if (!termino) return this.productos();

    return this.productos().filter(p =>
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

  cargarProductos(): void {
    this.isLoading.set(true);
    this.productoService.listar().subscribe({
      next: (data) => {
        this.productos.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando catálogo de productos:', err);
        this.isLoading.set(false);
        this.snackBar.open('No se pudo cargar el catálogo de productos.', 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  esStockCritico(producto: ProductoResponse): boolean {
    return (producto.stockTotal ?? 0) <= (producto.stockMinimo ?? 0);
  }

  eliminar(id: number, nombre: string): void {
    const confirmado = confirm(
      `¿Estás seguro de eliminar el producto "${nombre}"?\n` +
      `Nota: Si el producto tiene historial en producción o inventario, la operación será rechazada por el sistema.`
    );

    if (!confirmado) return;

    this.productoService.eliminar(id).subscribe({
      next: () => {
        this.productos.update(actuales => actuales.filter(p => p.id !== id));
        this.snackBar.open(`Producto "${nombre}" eliminado correctamente.`, 'Cerrar', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        console.error(err);
        const mensaje = err.error?.message || 'No se pudo eliminar. Es probable que el producto tenga stock o historial en uso.';
        this.snackBar.open(mensaje, 'Entendido', {
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