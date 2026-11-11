import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  TransaccionFinanciera,
  TransaccionFinancieraRequest,
} from '../models/transaccion-financiera.model';

@Injectable({ providedIn: 'root' })
export class TransaccionFinancieraService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/finanzas/transacciones`;

  listar(categoriaFinancieraId?: number): Observable<TransaccionFinanciera[]> {
    let params = new HttpParams();
    if (categoriaFinancieraId) {
      params = params.set('categoriaFinancieraId', categoriaFinancieraId);
    }
    return this.http.get<TransaccionFinanciera[]>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<TransaccionFinanciera> {
    return this.http.get<TransaccionFinanciera>(`${this.apiUrl}/${id}`);
  }

  crear(request: TransaccionFinancieraRequest): Observable<TransaccionFinanciera> {
    return this.http.post<TransaccionFinanciera>(this.apiUrl, request);
  }

  actualizar(id: number, request: TransaccionFinancieraRequest): Observable<TransaccionFinanciera> {
    return this.http.put<TransaccionFinanciera>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
