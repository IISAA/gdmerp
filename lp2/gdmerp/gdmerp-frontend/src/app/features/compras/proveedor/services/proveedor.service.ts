import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  Proveedor,
  ProveedorRequest,
  RucConsultaResponse,
} from '../models/proveedor.model';

@Injectable({ providedIn: 'root' })
export class ProveedorService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/compras/proveedores`;
  private readonly personasUrl = `${environment.apiUrl}/personas`;

  listarProveedores(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(this.apiUrl);
  }

  crearProveedor(request: ProveedorRequest): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.apiUrl, request);
  }

  consultarRuc(ruc: string): Observable<RucConsultaResponse> {
    return this.http.get<RucConsultaResponse>(`${this.personasUrl}/ruc/${ruc}`);
  }
}
