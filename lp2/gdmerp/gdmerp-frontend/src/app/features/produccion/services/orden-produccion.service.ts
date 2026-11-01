import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  OrdenProduccionRequest,
  OrdenProduccionUpdateRequest,
  OrdenProduccionResponse,
  EstadoOrdenProduccion
} from '../models/produccion.models';

@Injectable({
  providedIn: 'root'
})
export class OrdenProduccionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ordenes-produccion`;

  // Estado reactivo basado en Signals
  readonly ordenes = signal<OrdenProduccionResponse[]>([]);
  readonly isLoading = signal<boolean>(false);

  /**
   * Obtiene la lista de órdenes de producción y actualiza el signal.
   */
  listar(): void {
    this.isLoading.set(true);
    this.http.get<OrdenProduccionResponse[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.ordenes.set(data);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Error al listar órdenes de producción:', err);
        this.isLoading.set(false);
      }
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
  actualizar(id: number, request: OrdenProduccionUpdateRequest | OrdenProduccionRequest): Observable<OrdenProduccionResponse> {
    return this.http.put<OrdenProduccionResponse>(`${this.apiUrl}/${id}`, request).pipe(
      tap(() => this.listar())
    );
  }

  /**
   * Cambia el estado de una orden de producción respetando las reglas de la máquina de estados.
   */
  cambiarEstado(id: number, nuevoEstado: EstadoOrdenProduccion | string): Observable<OrdenProduccionResponse> {
    return this.http.patch<OrdenProduccionResponse>(`${this.apiUrl}/${id}/estado`, null, {
      params: { nuevoEstado }
    }).pipe(
      tap(() => this.listar())
    );
  }

  /**
   * Registra el ingreso de inventario del producto terminado de una orden completada al stock del almacén.
   */
  ingresarAlmacen(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/ingresar-almacen`, {});
  }

  /**
   * Elimina una orden de producción por ID y recarga el signal.
   */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.listar())
    );
  }

  /**
   * Obtiene el reporte oficial de auditoría de la orden en formato binario PDF.
   */
  obtenerReportePdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/reporte-pdf`, {
      responseType: 'blob'
    });
  }
}
