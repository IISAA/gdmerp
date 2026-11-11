import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CategoriaFinancieraService } from '../services/categoria-financiera.service';
import { CategoriaFinanciera } from '../models/categoria-financiera.model';

@Component({
  selector: 'app-categoria-financiera-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './categoria-financiera-list.component.html',
})
export class CategoriaFinancieraList implements OnInit {
  private readonly categoriaService = inject(CategoriaFinancieraService);

  protected readonly categorias = signal<CategoriaFinanciera[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.categoriaService.listar().subscribe({
      next: (data) => this.categorias.set(data),
      error: () => {
        this.error.set('No se pudo cargar la lista de categorías financieras.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  eliminar(id: number): void {
    if (!confirm(`¿Está seguro de eliminar la categoría financiera ${id}?`)) {
      return;
    }

    this.categoriaService.eliminar(id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.error.set('La categoría ya no existe. Se recargó la lista.');
          this.cargar();
        } else {
          this.error.set('No se pudo eliminar la categoría financiera.');
        }
      },
    });
  }
}
