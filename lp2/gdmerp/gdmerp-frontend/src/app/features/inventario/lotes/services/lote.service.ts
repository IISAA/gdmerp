import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { LoteRequest, LoteResponse } from '../models/lote.model';

@Injectable({ providedIn: 'root' })
export class LoteService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = `${environment.apiUrl}/lotes`;

    obtenerSiguienteCodigo(): Observable<{ codigo: string }> {
        return this.http.get<{ codigo: string }>(`${this.apiUrl}/siguiente-codigo`);
    }

    listar(productoId?: number): Observable<LoteResponse[]> {
        if (productoId != null) {
            return this.listarPorProducto(productoId);
        }
        const params = new HttpParams().set('sort', 'fechaVencimiento,asc');
        return this.http.get<LoteResponse[]>(this.apiUrl, { params });
    }

    listarPorProducto(productoId: number): Observable<LoteResponse[]> {
        return this.http.get<LoteResponse[]>(`${this.apiUrl}/producto/${productoId}`);
    }

    registrarIngreso(request: LoteRequest): Observable<LoteResponse> {
        return this.http.post<LoteResponse>(this.apiUrl, request);
    }
}