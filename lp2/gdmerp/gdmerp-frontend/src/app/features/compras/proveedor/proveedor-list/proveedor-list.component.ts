import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProveedorService } from '../services/proveedor.service';
import { Proveedor } from '../models/proveedor.model';

@Component({
  selector: 'app-proveedor-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './proveedor-list.component.html',
  styleUrl: './proveedor-list.component.css',
})
export class ProveedorList implements OnInit {
  private readonly proveedorService = inject(ProveedorService);

  protected readonly proveedores = signal<Proveedor[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.proveedorService.listarProveedores().subscribe({
      next: (data: Proveedor[]) => {
        this.proveedores.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de proveedores.');
        this.loading.set(false);
      },
    });
  }
}
