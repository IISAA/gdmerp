import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
    KardexFiltro,
    MovimientoInventarioRequest,
    MovimientoInventarioResponse,
} from '../models/movimiento-inventario.model';

@Injectable({
    providedIn: 'root',
})
export class MovimientoInventarioService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = `${environment.apiUrl}/movimientos`;

    listarKardex(filtro?: KardexFiltro): Observable<MovimientoInventarioResponse[]> {
        let params = new HttpParams();

        if (filtro?.productoId) {
            params = params.set('productoId', filtro.productoId.toString());
        }
        if (filtro?.tipoMovimiento) {
            params = params.set('tipoMovimiento', filtro.tipoMovimiento);
        }
        if (filtro?.desde) {
            params = params.set('desde', filtro.desde);
        }
        if (filtro?.hasta) {
            params = params.set('hasta', filtro.hasta);
        }

        return this.http.get<MovimientoInventarioResponse[]>(this.apiUrl, { params });
    }

    registrarMovimiento(
        request: MovimientoInventarioRequest
    ): Observable<MovimientoInventarioResponse> {
        return this.http.post<MovimientoInventarioResponse>(this.apiUrl, request);
    }
}