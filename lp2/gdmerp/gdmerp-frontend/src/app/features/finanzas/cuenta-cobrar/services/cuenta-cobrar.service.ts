import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  CuentaPorCobrar,
  CuentaPorCobrarDetalle,
  CrearCreditoRequest,
  RegistrarAbonoRequest,
  EstadoCredito,
} from '../models/cuenta-cobrar.model';

@Injectable({ providedIn: 'root' })
export class CuentaCobrarService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/finanzas/creditos`;

  listar(estado?: EstadoCredito): Observable<CuentaPorCobrar[]> {
    let params = new HttpParams();
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<CuentaPorCobrar[]>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<CuentaPorCobrarDetalle> {
    return this.http.get<CuentaPorCobrarDetalle>(`${this.apiUrl}/${id}`);
  }

  registrarCredito(request: CrearCreditoRequest): Observable<CuentaPorCobrar> {
    return this.http.post<CuentaPorCobrar>(this.apiUrl, request);
  }

  registrarAbono(id: number, request: RegistrarAbonoRequest): Observable<CuentaPorCobrarDetalle> {
    return this.http.post<CuentaPorCobrarDetalle>(`${this.apiUrl}/${id}/abonos`, request);
  }
}
