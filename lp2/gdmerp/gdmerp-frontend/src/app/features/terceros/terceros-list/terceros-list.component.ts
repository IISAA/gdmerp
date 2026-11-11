import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TercerosService } from '../services/terceros.service';
import { Tercero, TipoTercero } from '../models/terceros.model';

@Component({
  selector: 'app-terceros-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './terceros-list.component.html',
  styleUrl: './terceros-list.component.css',
})
export class TercerosList implements OnInit {
  private readonly tercerosService = inject(TercerosService);

  readonly terceros = signal<Tercero[]>([]);
  readonly cargando = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly filtroTipo = signal<string>('TODOS');
  readonly terminoBusqueda = signal<string>('');

  ngOnInit(): void {
    this.cargarTerceros();
  }

  cargarTerceros(): void {
    this.cargando.set(true);
    const tipo = this.filtroTipo() === 'TODOS' ? undefined : (this.filtroTipo() as TipoTercero);
    this.tercerosService.listar(tipo).subscribe({
      next: (data: Tercero[]) => {
        this.terceros.set(data);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as { message?: string } | null)?.message;
        this.error.set('Error al cargar directorio de terceros: ' + (msg || err.message));
        this.cargando.set(false);
      },
    });
  }

  cambiarFiltro(tipo: string): void {
    this.filtroTipo.set(tipo);
    this.cargarTerceros();
  }

  get tercerosFiltrados(): Tercero[] {
    const q = this.terminoBusqueda().toLowerCase().trim();
    if (!q) return this.terceros();
    return this.terceros().filter(
      (t: Tercero) =>
        t.razonSocial.toLowerCase().includes(q) ||
        t.numeroDocumento.toLowerCase().includes(q) ||
        (t.direccion && t.direccion.toLowerCase().includes(q))
    );
  }
}
