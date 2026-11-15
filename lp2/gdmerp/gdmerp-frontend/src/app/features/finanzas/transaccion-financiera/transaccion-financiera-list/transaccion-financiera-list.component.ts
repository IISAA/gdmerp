import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { CategoriaFinancieraService } from '../../categoria-financiera/services/categoria-financiera.service';
import { CategoriaFinanciera } from '../../categoria-financiera/models/categoria-financiera.model';
import { TransaccionFinancieraService } from '../services/transaccion-financiera.service';
import { TransaccionFinanciera } from '../models/transaccion-financiera.model';

@Component({
  selector: 'app-transaccion-financiera-list',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, MatSnackBarModule, MatIconModule],
  templateUrl: './transaccion-financiera-list.component.html',
})
export class TransaccionFinancieraList implements OnInit {
  private readonly transaccionService = inject(TransaccionFinancieraService);
  private readonly categoriaService = inject(CategoriaFinancieraService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly transacciones = signal<TransaccionFinanciera[]>([]);
  protected readonly categorias = signal<CategoriaFinanciera[]>([]);
  protected readonly categoriaFiltro = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.categoriaService.listar().subscribe({
      next: (data) => this.categorias.set(data),
      error: () => this.mostrarMensaje('No se pudo cargar la lista de categorías financieras.', 'error'),
    });
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.transaccionService.listar(this.categoriaFiltro() ?? undefined).subscribe({
      next: (data) => this.transacciones.set(data),
      error: () => {
        this.error.set('No se pudo cargar la lista de transacciones financieras.');
        this.loading.set(false);
        this.mostrarMensaje('Error al cargar transacciones financieras.', 'error');
      },
      complete: () => this.loading.set(false),
    });
  }

  filtrar(categoriaId: number): void {
    this.categoriaFiltro.set(categoriaId || null);
    this.cargar();
  }

  eliminar(id: number, codigo: string): void {
    if (!confirm(`¿Está seguro de eliminar la transacción financiera ${codigo} (#${id})?`)) {
      return;
    }

    this.transaccionService.eliminar(id).subscribe({
      next: () => {
        this.mostrarMensaje(`Transacción ${codigo} eliminada correctamente.`, 'success');
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.mostrarMensaje('La transacción ya no existe en el sistema. Lista actualizada.', 'warning');
          this.cargar();
        } else {
          this.mostrarMensaje('No se pudo eliminar la transacción.', 'error');
        }
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
