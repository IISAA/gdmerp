import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { AlmacenResponse, AlmacenRequest } from '../models/almacen.model';

@Injectable({
  providedIn: 'root'
})
export class AlmacenService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment?.apiUrl || 'http://localhost:8080/api/v1'}/almacenes`;

  listar(): Observable<AlmacenResponse[]> {
    return this.http.get<AlmacenResponse[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<AlmacenResponse> {
    return this.http.get<AlmacenResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: AlmacenRequest): Observable<AlmacenResponse> {
    return this.http.post<AlmacenResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: AlmacenRequest): Observable<AlmacenResponse> {
    return this.http.put<AlmacenResponse>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
