import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  AsignacionCajaCentroCosto,
  AsignarPresupuestoRequest,
  CajaChica,
  CentroCosto,
  PageResponse,
  RendicionCajaChica,
  RendicionCajaChicaRequest,
  ReporteRendicionesAgregado,
} from '../models/rendicion-caja-chica.model';

@Injectable({ providedIn: 'root' })
export class RendicionCajaChicaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/finanzas`;

  listarCajasChicas(): Observable<CajaChica[]> {
    return this.http.get<CajaChica[]>(`${this.baseUrl}/cajas-chicas`);
  }

  listarCentrosCosto(): Observable<CentroCosto[]> {
    return this.http.get<CentroCosto[]>(`${this.baseUrl}/centros-costo`);
  }

  crearCentroCosto(data: {
    codigoCc: string;
    nombreArea: string;
  }): Observable<CentroCosto> {
    return this.http.post<CentroCosto>(`${this.baseUrl}/centros-costo`, data);
  }

  listarPresupuestosCaja(cajaChicaId: number): Observable<AsignacionCajaCentroCosto[]> {
    return this.http.get<AsignacionCajaCentroCosto[]>(`${this.baseUrl}/cajas-chicas/${cajaChicaId}/presupuestos`);
  }

  asignarPresupuesto(
    cajaChicaId: number,
    data: AsignarPresupuestoRequest
  ): Observable<AsignacionCajaCentroCosto> {
    return this.http.post<AsignacionCajaCentroCosto>(`${this.baseUrl}/cajas-chicas/${cajaChicaId}/presupuestos`, data);
  }

  crearCajaChica(datos: {
    codigo: string;
    responsable: string;
    limiteMaximo: number;
    saldoInicial: number;
  }): Observable<CajaChica> {
    const params = new HttpParams()
      .set('codigo', datos.codigo)
      .set('responsable', datos.responsable)
      .set('limiteMaximo', datos.limiteMaximo.toString())
      .set('saldoInicial', datos.saldoInicial.toString());
    return this.http.post<CajaChica>(`${this.baseUrl}/cajas-chicas`, null, { params });
  }

  obtenerCajaChica(id: number): Observable<CajaChica> {
    return this.http.get<CajaChica>(`${this.baseUrl}/cajas-chicas/${id}`);
  }

  registrarRendicion(request: RendicionCajaChicaRequest): Observable<RendicionCajaChica> {
    return this.http.post<RendicionCajaChica>(`${this.baseUrl}/rendiciones`, request);
  }

  obtenerRendicion(id: number): Observable<RendicionCajaChica> {
    return this.http.get<RendicionCajaChica>(`${this.baseUrl}/rendiciones/${id}`);
  }

  buscarConFiltros(filtros: {
    cajaChicaId?: number | null;
    estado?: string | null;
    responsable?: string | null;
    fechaInicio?: string | null;
    fechaFin?: string | null;
    montoMin?: number | null;
    montoMax?: number | null;
    page?: number;
    size?: number;
  }): Observable<PageResponse<RendicionCajaChica>> {
    let params = new HttpParams();
    if (filtros.cajaChicaId) params = params.set('cajaChicaId', filtros.cajaChicaId.toString());
    if (filtros.estado) params = params.set('estado', filtros.estado);
    if (filtros.responsable) params = params.set('responsable', filtros.responsable);
    if (filtros.fechaInicio) params = params.set('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) params = params.set('fechaFin', filtros.fechaFin);
    if (filtros.montoMin != null) params = params.set('montoMin', filtros.montoMin.toString());
    if (filtros.montoMax != null) params = params.set('montoMax', filtros.montoMax.toString());
    params = params.set('page', (filtros.page ?? 0).toString());
    params = params.set('size', (filtros.size ?? 20).toString());

    return this.http.get<PageResponse<RendicionCajaChica>>(`${this.baseUrl}/rendiciones/filtros`, { params });
  }

  obtenerReporteAgregado(cajaChicaId?: number | null): Observable<ReporteRendicionesAgregado> {
    let params = new HttpParams();
    if (cajaChicaId) {
      params = params.set('cajaChicaId', cajaChicaId.toString());
    }
    return this.http.get<ReporteRendicionesAgregado>(`${this.baseUrl}/rendiciones/reporte-agregado`, { params });
  }

  anularRendicion(id: number): Observable<RendicionCajaChica> {
    return this.http.post<RendicionCajaChica>(`${this.baseUrl}/rendiciones/${id}/anular`, {});
  }
}
