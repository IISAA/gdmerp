import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { UnidadMedida, UnidadMedidaRequest } from '../models/unidad-medida.model';

export interface SincronizacionEstandaresResponse {
  insertadas: number;
  omitidas: number;
  codigosNuevos: string[];
  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class UnidadMedidaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/inventario/unidades-medida`;

  /**
   * Obtiene todas las unidades de medida desde el backend (GET /api/v1/inventario/unidades-medida).
   */
  obtenerTodas(): Observable<UnidadMedida[]> {
    return this.http.get<UnidadMedida[]>(this.apiUrl);
  }

  listar(soloActivos?: boolean): Observable<UnidadMedida[]> {
    let params = new HttpParams();
    if (soloActivos !== undefined) {
      params = params.set('activo', soloActivos);
    }
    return this.http.get<UnidadMedida[]>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<UnidadMedida> {
    return this.http.get<UnidadMedida>(`${this.apiUrl}/${id}`);
  }

  crear(request: UnidadMedidaRequest): Observable<UnidadMedida> {
    return this.http.post<UnidadMedida>(this.apiUrl, request);
  }

  actualizar(id: number, request: UnidadMedidaRequest): Observable<UnidadMedida> {
    return this.http.put<UnidadMedida>(`${this.apiUrl}/${id}`, request);
  }

  cambiarEstado(id: number, activo: boolean): Observable<UnidadMedida> {
    return this.http.patch<UnidadMedida>(`${this.apiUrl}/${id}/estado`, null, {
      params: new HttpParams().set('activo', activo)
    });
  }

  /** POST /cargar-estandares — sincronización idempotente del catálogo SI/UN-ECE/SUNAT */
  cargarEstandares(): Observable<SincronizacionEstandaresResponse> {
    return this.http.post<SincronizacionEstandaresResponse>(`${this.apiUrl}/cargar-estandares`, null);
  }
}
