import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  AperturaSesionRequest,
  CierreCajaReporte,
  CierreSesionRequest,
  DenominacionMoneda,
  MovimientoCaja,
  MovimientoCajaRequest,
  SesionCaja,
  SesionCajaDetalle,
} from '../models/sesion-caja.model';

@Injectable({ providedIn: 'root' })
export class SesionCajaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/finanzas/sesiones`;
  private readonly denominacionesUrl = `${environment.apiUrl}/finanzas/denominaciones`;
  private readonly reportesUrl = `${environment.apiUrl}/finanzas/reportes`;

  listar(): Observable<SesionCaja[]> {
    return this.http.get<SesionCaja[]>(this.baseUrl);
  }

  listarDenominaciones(): Observable<DenominacionMoneda[]> {
    return this.http.get<DenominacionMoneda[]>(this.denominacionesUrl);
  }

  obtenerDetalle(id: number): Observable<SesionCajaDetalle> {
    return this.http.get<SesionCajaDetalle>(`${this.baseUrl}/${id}`);
  }

  obtenerActiva(): Observable<SesionCaja> {
    return this.http.get<SesionCaja>(`${this.baseUrl}/activa`);
  }

  abrir(request: AperturaSesionRequest): Observable<SesionCaja> {
    return this.http.post<SesionCaja>(`${this.baseUrl}/abrir`, request);
  }

  cerrar(id: number, request: CierreSesionRequest): Observable<SesionCaja> {
    return this.http.post<SesionCaja>(`${this.baseUrl}/${id}/cerrar`, request);
  }

  registrarMovimiento(id: number, request: MovimientoCajaRequest): Observable<MovimientoCaja> {
    return this.http.post<MovimientoCaja>(`${this.baseUrl}/${id}/movimientos`, request);
  }

  obtenerDatosCierre(id: number): Observable<CierreCajaReporte> {
    return this.http.get<CierreCajaReporte>(`${this.baseUrl}/${id}/datos-cierre`);
  }

  exportar(id: number, formato: 'PDF' | 'EXCEL'): Observable<Blob> {
    const params = new HttpParams().set('formato', formato);
    return this.http.get(`${this.reportesUrl}/cierre-caja/${id}/exportar`, {
      params,
      responseType: 'blob',
    });
  }
}
