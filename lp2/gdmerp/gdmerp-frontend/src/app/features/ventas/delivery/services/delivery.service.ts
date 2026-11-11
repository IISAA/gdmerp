import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  PedidoDelivery,
  CrearDeliveryRequest,
  ActualizarEstadoDeliveryRequest,
  AsignarRepartidorRequest,
  EstadoDelivery,
} from '../models/delivery.model';

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ventas/delivery`;

  listar(estado?: EstadoDelivery): Observable<PedidoDelivery[]> {
    let params = new HttpParams();
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<PedidoDelivery[]>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<PedidoDelivery> {
    return this.http.get<PedidoDelivery>(`${this.apiUrl}/${id}`);
  }

  crearPedido(req: CrearDeliveryRequest): Observable<PedidoDelivery> {
    return this.http.post<PedidoDelivery>(this.apiUrl, req);
  }

  actualizarEstado(id: number, req: ActualizarEstadoDeliveryRequest): Observable<PedidoDelivery> {
    return this.http.patch<PedidoDelivery>(`${this.apiUrl}/${id}/estado`, req);
  }

  asignarRepartidor(id: number, req: AsignarRepartidorRequest): Observable<PedidoDelivery> {
    return this.http.patch<PedidoDelivery>(`${this.apiUrl}/${id}/repartidor`, req);
  }
}
