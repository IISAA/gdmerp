import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

// Angular Material
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { HasRoleDirective } from '../../../../shared/directives/has-role.directive';
import { ListaMaterialesService } from '../../services/lista-materiales.service';
import { ListaMaterialesResponse } from '../../models/lista-materiales.model';

@Component({
  selector: 'app-lista-materiales-list',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatProgressSpinnerModule,
    HasRoleDirective
  ],
  templateUrl: './lista-materiales-list.component.html',
  styleUrls: ['./lista-materiales-list.component.scss']
})
export class ListaMaterialesListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly listaMaterialesService = inject(ListaMaterialesService);

  readonly listas = signal<ListaMaterialesResponse[]>([]);
  readonly cargando = signal<boolean>(true);
  readonly textoBusqueda = signal<string>('');

  readonly displayedColumns: string[] = [
    'codigo',
    'producto',
    'cantidadBase',
    'numInsumos'
  ];

  readonly listasFiltradas = computed(() => {
    const query = this.textoBusqueda().trim().toLowerCase();
    const data = this.listas();
    if (!query) {
      return data;
    }
    return data.filter(item =>
      (item.codigo && item.codigo.toLowerCase().includes(query)) ||
      (item.nombreProductoTerminado && item.nombreProductoTerminado.toLowerCase().includes(query)) ||
      (item.skuProductoTerminado && item.skuProductoTerminado.toLowerCase().includes(query))
    );
  });

  ngOnInit(): void {
    this.cargarListas();
  }

  cargarListas(): void {
    this.cargando.set(true);
    this.listaMaterialesService.listar().subscribe({
      next: (data) => {
        this.listas.set(data || []);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar listas de materiales:', err);
        this.cargando.set(false);
      }
    });
  }

  onBuscar(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.textoBusqueda.set(input.value);
  }

  limpiarBusqueda(): void {
    this.textoBusqueda.set('');
  }

  irADetalle(id: number): void {
    this.router.navigate(['/produccion/listas-materiales', id]);
  }
}
