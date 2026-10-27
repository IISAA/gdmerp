import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { 
  ListaMaterialesDto, 
  ListaMaterialesRequest, 
  ListaMaterialesResponse 
} from '../models/lista-materiales.model';

@Injectable({
  providedIn: 'root'
})
export class ListaMaterialesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/listas-materiales`;

  listar(): Observable<ListaMaterialesResponse[]> {
    return this.http.get<ListaMaterialesResponse[]>(this.apiUrl);
  }

  obtenerPorId(id: number): Observable<ListaMaterialesResponse> {
    return this.http.get<ListaMaterialesResponse>(`${this.apiUrl}/${id}`);
  }

  obtenerPorProductoTerminado(productoTerminadoId: number): Observable<ListaMaterialesResponse[]> {
    return this.http.get<ListaMaterialesResponse[]>(`${this.apiUrl}/por-producto/${productoTerminadoId}`);
  }

  obtenerPorProducto(productoId: number): Observable<ListaMaterialesResponse[]> {
    return this.http.get<ListaMaterialesResponse[]>(`${this.apiUrl}/por-producto/${productoId}`);
  }

  crear(request: ListaMaterialesRequest | ListaMaterialesDto): Observable<ListaMaterialesResponse> {
    return this.http.post<ListaMaterialesResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: ListaMaterialesRequest | ListaMaterialesDto): Observable<ListaMaterialesResponse> {
    return this.http.put<ListaMaterialesResponse>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
