import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface StockCriticoDto {
  productoId?: number;
  id?: number;
  sku?: string;
  nombreProducto?: string;
  nombre?: string;
  stockActual?: number;
  stockFisico?: number;
  stockMinimo: number;
  estado?: string;
}

export interface StockConsolidadoDto {
  productoId?: number;
  id?: number;
  sku?: string;
  nombreProducto?: string;
  nombre?: string;
  categoria?: string;
  almacen?: string;
  stockTotal?: number;
  stockFisico?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductoReporteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/productos/reportes`;

  obtenerStockCritico(): Observable<StockCriticoDto[]> {
    return this.http.get<StockCriticoDto[]>(`${this.apiUrl}/stock-critico`);
  }

  obtenerStockConsolidado(): Observable<StockConsolidadoDto[]> {
    return this.http.get<StockConsolidadoDto[]>(`${this.apiUrl}/stock`);
  }
}
