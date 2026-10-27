import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface OrdenesPorEstadoDto {
  estado: string;
  cantidadOrdenes: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrdenReporteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/produccion/ordenes/reportes`;

  obtenerResumenPorEstado(): Observable<OrdenesPorEstadoDto[]> {
    return this.http.get<OrdenesPorEstadoDto[]>(`${this.apiUrl}/estados`);
  }
}
