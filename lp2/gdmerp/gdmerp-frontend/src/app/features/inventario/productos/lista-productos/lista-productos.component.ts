import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProductoService } from '../services/producto.service';
import { ProductoResponse } from '../models/producto.model';

@Component({
  selector: 'app-lista-productos',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink, FormsModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './lista-productos.component.html',
  styleUrls: ['./lista-productos.component.css'] // O .scss según tu configuración
})
export class ListaProductosComponent implements OnInit {
  private readonly productoService = inject(ProductoService);

  // Estado principal
  protected productos = signal<ProductoResponse[]>([]);
  protected textoBusqueda = signal<string>('');
  protected isLoading = signal<boolean>(false);

  // Filtro Reactivo
  protected productosFiltrados = computed(() => {
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
      }
    });
  }

  onBuscar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
  }

  eliminar(id: number, nombre: string): void {
    if (confirm(`¿Estás seguro de eliminar el producto "${nombre}"?\nNota: Si el producto tiene historial en producción o inventario, la operación será rechazada por el sistema.`)) {
      this.productoService.eliminar(id).subscribe({
        next: () => {
          // Actualizar la señal removiendo el elemento eliminado
          this.productos.update(actuales => actuales.filter(p => p.id !== id));
        },
        error: (err) => {
          alert('No se pudo eliminar. Es probable que el producto ya esté en uso (Restricción de Integridad).');
          console.error(err);
        }
      });
    }
  }
}

export { ListaProductosComponent as ProductoListComponent };

