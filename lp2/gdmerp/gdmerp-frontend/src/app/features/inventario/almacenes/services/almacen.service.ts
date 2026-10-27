import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Almacen, AlmacenRequest } from '../models/almacen.model';

@Injectable({
  providedIn: 'root'
})
export class AlmacenService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/almacenes`;

  listar(): Observable<Almacen[]> {
    return this.http.get<Almacen[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Almacen> {
    return this.http.get<Almacen>(`${this.apiUrl}/${id}`);
  }

  crear(request: AlmacenRequest): Observable<Almacen> {
    return this.http.post<Almacen>(this.apiUrl, request);
  }

  actualizar(id: number, request: AlmacenRequest): Observable<Almacen> {
    return this.http.put<Almacen>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
