import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { CentroTrabajo, CentroTrabajoRequest } from '../models/centro-trabajo.model';

@Injectable({
  providedIn: 'root'
})
export class CentroTrabajoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/centros-trabajo`;

  listar(): Observable<CentroTrabajo[]> {
    return this.http.get<CentroTrabajo[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<CentroTrabajo> {
    return this.http.get<CentroTrabajo>(`${this.apiUrl}/${id}`);
  }

  crear(request: CentroTrabajoRequest): Observable<CentroTrabajo> {
    return this.http.post<CentroTrabajo>(this.apiUrl, request);
  }

  actualizar(id: number, request: CentroTrabajoRequest): Observable<CentroTrabajo> {
    return this.http.put<CentroTrabajo>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
