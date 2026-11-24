import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  ListaMaterialesDto,
  ListaMaterialesRequest,
  ListaMaterialesResponse,
  SugerirCodigoBomResponse
} from '../model/lista-materiales.model';

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

  obtenerPorProducto(productoId: number): Observable<ListaMaterialesResponse[]> {
    return this.http.get<ListaMaterialesResponse[]>(`${this.apiUrl}/por-producto/${productoId}`);
  }

  obtenerPorProductoTerminado(productoTerminadoId: number): Observable<ListaMaterialesResponse[]> {
    return this.obtenerPorProducto(productoTerminadoId);
  }

  sugerirCodigo(productoTerminadoId: number): Observable<SugerirCodigoBomResponse> {
    const params = new HttpParams().set('productoTerminadoId', productoTerminadoId.toString());
    return this.http.get<SugerirCodigoBomResponse>(`${this.apiUrl}/sugerir-codigo`, { params });
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