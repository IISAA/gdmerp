import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  OrdenProduccionRequest,
  OrdenProduccionResponse
} from '../models/produccion.models';

@Injectable({
  providedIn: 'root'
})
export class OrdenProduccionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ordenes-produccion`;

  // Estado reactivo basado en Signals
  readonly ordenes = signal<OrdenProduccionResponse[]>([]);

  /**
   * Obtiene la lista de órdenes de producción y actualiza el signal.
   */
  listar(): void {
    this.http.get<OrdenProduccionResponse[]>(this.apiUrl).subscribe({
      next: (data) => this.ordenes.set(data),
      error: (err) => console.error('Error al listar órdenes de producción:', err)
    });
  }

  /**
   * Envía una nueva orden de producción al backend y recarga el signal.
   */
  crear(request: OrdenProduccionRequest): Observable<OrdenProduccionResponse> {
    return this.http.post<OrdenProduccionResponse>(this.apiUrl, request).pipe(
      tap(() => this.listar())
    );
  }

  // Obtener una orden específica para llenar el formulario de edición
  obtenerPorId(id: number) {
    return this.http.get<OrdenProduccionResponse>(`${this.apiUrl}/${id}`);
  }

  // Actualizar la orden y recargar la tabla
  actualizar(id: number, request: OrdenProduccionRequest) {
    return this.http.put<OrdenProduccionResponse>(`${this.apiUrl}/${id}`, request).pipe(
      tap(() => this.listar())
    );
  }

  /**
   * Elimina una orden de producción por ID y recarga el signal.
   */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.listar())
    );
  }
}
