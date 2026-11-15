import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ProveedorService } from '../services/proveedor.service';
import { Proveedor } from '../models/proveedor.model';

@Component({
  selector: 'app-proveedor-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatSnackBarModule, MatIconModule],
  templateUrl: './proveedor-list.component.html',
  styleUrl: './proveedor-list.component.css',
})
export class ProveedorList implements OnInit {
  private readonly proveedorService = inject(ProveedorService);
  private readonly snackBar = inject(MatSnackBar);

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
        this.mostrarMensaje('No se pudo cargar la lista de proveedores.', 'error');
      },
    });
  }

  private mostrarMensaje(mensaje: string, tipo: 'success' | 'error' | 'warning'): void {
    const panelClass = tipo === 'success' ? ['snackbar-success'] : tipo === 'error' ? ['snackbar-error'] : ['snackbar-warning'];
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass
    });
  }
}
