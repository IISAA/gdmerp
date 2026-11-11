import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Producto, ProductoResponse, ProductoRequest } from '../models/producto.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment?.apiUrl || 'http://localhost:8080/api/v1'}/productos`;

  listar(tipoProducto?: string): Observable<Producto[]> {
    let params = new HttpParams();
    if (tipoProducto) {
      params = params.set('tipoProducto', tipoProducto);
    }
    return this.http.get<Producto[]>(this.apiUrl, { params });
  }

  buscarPorId(id: number): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(`${this.apiUrl}/${id}`);
  }

  obtenerPorId(id: number): Observable<ProductoResponse> {
    return this.buscarPorId(id);
  }

  crear(request: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: ProductoRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  sugerirSku(almacenId: number, nombreProducto: string): Observable<{ skuSugerido: string }> {
    const params = new HttpParams()
      .set('almacenId', almacenId.toString())
      .set('nombreProducto', nombreProducto);
    return this.http.get<{ skuSugerido: string }>(`${this.apiUrl}/sugerir-sku`, { params });
  }
}
