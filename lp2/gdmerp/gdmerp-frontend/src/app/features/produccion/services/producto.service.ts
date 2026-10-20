import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Producto } from '../models/producto.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private readonly http = inject(HttpClient);
  // Asegúrate de que este endpoint exista en tu Spring Boot
  private readonly apiUrl = `${environment.apiUrl}/productos`; 

  listar(tipoProducto?: string): Observable<Producto[]> {
    let params = new HttpParams();
    if (tipoProducto) {
      params = params.set('tipoProducto', tipoProducto);
    }
    return this.http.get<Producto[]>(this.apiUrl, { params });
  }
}