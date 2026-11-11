import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  Compra,
  CompraRequest,
  AmortizarPagoRequest,
  CompraReporte,
  Insumo,
  PagoCompra,
} from '../models/compra.model';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/compras`;

  listarInsumos(): Observable<Insumo[]> {
    return this.http.get<Insumo[]>(`${this.apiUrl}/insumos`);
  }

  listarCompras(estado?: string, desde?: string, hasta?: string): Observable<Compra[]> {
    const params = this.buildFilterParams(estado, desde, hasta);
    return this.http.get<Compra[]>(this.apiUrl, { params });
  }

  crearCompra(request: CompraRequest): Observable<Compra> {
    return this.http.post<Compra>(this.apiUrl, request);
  }

  obtenerCompra(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.apiUrl}/${id}`);
  }

  amortizarPago(id: number, request: AmortizarPagoRequest): Observable<Compra> {
    return this.http.put<Compra>(`${this.apiUrl}/${id}/amortizar`, request);
  }

  listarPagos(id: number): Observable<PagoCompra[]> {
    return this.http.get<PagoCompra[]>(`${this.apiUrl}/${id}/pagos`);
  }

  anularCompra(id: number): Observable<Compra> {
    return this.http.post<Compra>(`${this.apiUrl}/${id}/anular`, {});
  }

  reporte(estado?: string, desde?: string, hasta?: string): Observable<CompraReporte> {
    const params = this.buildFilterParams(estado, desde, hasta);
    return this.http.get<CompraReporte>(`${this.apiUrl}/resumen`, { params });
  }

  private buildFilterParams(estado?: string, desde?: string, hasta?: string): HttpParams {
    let params = new HttpParams();
    if (estado && estado !== 'TODAS') {
      params = params.set('estado', estado);
    }
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return params;
  }
}
