import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { VentaRequest, VentaResponse, VentaReporte, PageResponse } from '../models/venta.model';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ventas`;
  private readonly personasUrl = `${environment.apiUrl}/personas`;

  consultarDni(dni: string): Observable<{ dni: string; nombres: string; apellidoPaterno: string; apellidoMaterno: string; nombreCompleto: string }> {
    return this.http.get<{ dni: string; nombres: string; apellidoPaterno: string; apellidoMaterno: string; nombreCompleto: string }>(
      `${this.personasUrl}/dni/${dni}`,
    );
  }

  crear(venta: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(this.apiUrl, venta);
  }

  buscar(
    estado?: string,
    metodoPago?: string,
    desde?: string,
    hasta?: string,
    page = 0,
    size = 50,
    sort = 'fecha,desc',
  ): Observable<VentaResponse[]> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString()).set('sort', sort);
    if (estado) params = params.set('estado', estado);
    if (metodoPago) params = params.set('metodoPago', metodoPago);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<any>(this.apiUrl, { params }).pipe(
      map(res => Array.isArray(res) ? res : (res?.content || []))
    );
  }

  buscarPaginado(
    estado?: string,
    metodoPago?: string,
    desde?: string,
    hasta?: string,
    page = 0,
    size = 20,
    sort = 'fecha,desc',
  ): Observable<PageResponse<VentaResponse>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString()).set('sort', sort);
    if (estado) params = params.set('estado', estado);
    if (metodoPago) params = params.set('metodoPago', metodoPago);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<PageResponse<VentaResponse>>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<VentaResponse> {
    return this.http.get<VentaResponse>(`${this.apiUrl}/${id}`);
  }

  anular(id: number): Observable<VentaResponse> {
    return this.http.patch<VentaResponse>(`${this.apiUrl}/${id}/anular`, {});
  }

  solicitarAnulacion(id: number, motivo?: string): Observable<VentaResponse> {
    let params = new HttpParams();
    if (motivo) {
      params = params.set('motivo', motivo);
    }
    return this.http.patch<VentaResponse>(`${this.apiUrl}/${id}/solicitar-anulacion`, {}, { params });
  }

  rechazarAnulacion(id: number): Observable<VentaResponse> {
    return this.http.patch<VentaResponse>(`${this.apiUrl}/${id}/rechazar-anulacion`, {});
  }

  reporte(estado?: string, metodoPago?: string, desde?: string, hasta?: string): Observable<VentaReporte> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    if (metodoPago) params = params.set('metodoPago', metodoPago);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<VentaReporte>(`${this.apiUrl}/resumen`, { params });
  }
}
