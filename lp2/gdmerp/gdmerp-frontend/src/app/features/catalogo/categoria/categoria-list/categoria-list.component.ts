import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { CategoriaService } from '../services/categoria.service';
import { Categoria } from '../models/categoria.model';

@Component({
  selector: 'app-categoria-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './categoria-list.component.html',
  styleUrl: './categoria-list.component.css',
})
export class CategoriaList implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly categoriaService = inject(CategoriaService);

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly successMsg = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.categoriaService.listar().subscribe({
      next: (data: Categoria[]) => {
        this.categorias.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de categorías desde el backend.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  eliminar(categoria: Categoria): void {
    if (!confirm(`¿Eliminar la categoría "${categoria.nombre}"?`)) {
      return;
    }

    this.loading.set(true);
    this.categoriaService.eliminar(categoria.id).subscribe({
      next: () => {
        this.successMsg.set(`Categoría "${categoria.nombre}" eliminada correctamente.`);
        this.cargarDatos();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message;
        this.error.set(msg || 'No se pudo eliminar la categoría.');
        this.loading.set(false);
      },
    });
  }
}
