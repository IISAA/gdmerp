import { CurrencyPipe, CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { CategoriaService } from '../../categoria/services/categoria.service';
import { Categoria } from '../../categoria/models/categoria.model';
import { ProductoService } from '../services/producto.service';
import { Producto } from '../models/producto.model';

@Component({
  selector: 'app-producto-list',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, FormsModule],
  templateUrl: './producto-list.component.html',
  styleUrl: './producto-list.component.css',
})
export class ProductoList implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly productoService = inject(ProductoService);
  private readonly categoriaService = inject(CategoriaService);

  protected readonly productos = signal<Producto[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly categoriaFiltro = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly productoSeleccionado = signal<Producto | null>(null);

  ngOnInit(): void {
    this.categoriaService.listar().subscribe({
      next: (data: Categoria[]) => this.categorias.set(data),
      error: () => this.error.set('No se pudo cargar la lista de categorías para el filtro.'),
    });
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    const filtro = this.categoriaFiltro() ?? undefined;

    this.productoService.listar(filtro).subscribe({
      next: (data: Producto[]) => {
        this.productos.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de productos.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  onFiltroChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    const catId = val ? Number(val) : null;
    this.categoriaFiltro.set(catId);
    this.cargar();
  }

  getImagen(producto: Producto): string {
    return producto.imagenUrl && producto.imagenUrl.trim().length > 0
      ? producto.imagenUrl
      : `/assets/productos/producto-${producto.id}.png`;
  }

  abrirModalVista(producto: Producto): void {
    this.productoSeleccionado.set(producto);
  }

  cerrarModalVista(): void {
    this.productoSeleccionado.set(null);
  }

  eliminar(producto: Producto): void {
    if (!confirm(`¿Está seguro de eliminar el producto "${producto.nombre}"?`)) {
      return;
    }

    this.loading.set(true);
    this.productoService.eliminar(producto.id).subscribe({
      next: () => {
        this.successMsg.set(`Producto "${producto.nombre}" eliminado correctamente.`);
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message;
        this.error.set(msg || 'No se pudo eliminar el producto.');
        this.loading.set(false);
      },
    });
  }
}
