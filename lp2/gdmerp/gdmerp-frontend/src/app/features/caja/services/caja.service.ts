import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  SesionCaja,
  MovimientoCaja,
  MovimientoCajaRequest,
  AperturaCajaRequest,
  CierreCajaRequest,
  CajaReporte,
} from '../models/caja.model';

interface BackendSesionCajaResponse {
  id: number;
  codigoSesion: string;
  usuarioId: number;
  cajero: string;
  cajeroRol?: string;
  fechaApertura: string;
  fechaCierre?: string | null;
  saldoInicial: number;
  totalIngresosEfectivo: number;
  totalEgresosEfectivo: number;
  saldoEsperadoEfectivo: number;
  saldoRealEfectivo?: number | null;
  diferencia?: number | null;
  estado: 'ABIERTA' | 'CERRADA';
  observaciones?: string | null;
}

interface BackendCategoriaFinanciera {
  id: number;
  nombre: string;
  tipoDefault: 'INGRESO' | 'EGRESO';
}

interface BackendMovimientoCajaResponse {
  id: number;
  fechaHora: string;
  tipo: 'INGRESO' | 'EGRESO';
  metodoPago: 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA';
  monto: number;
  descripcion: string;
  referenciaExterna?: string;
  sesionCajaId: number;
}

@Injectable({ providedIn: 'root' })
export class CajaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/finanzas/sesiones`;
  private readonly categoriasUrl = `${environment.apiUrl}/finanzas/categorias`;

  private mapSesion(s: BackendSesionCajaResponse): SesionCaja {
    return {
      id: s.id,
      cajeroNombre: s.cajero,
      fechaApertura: s.fechaApertura,
      fechaCierre: s.fechaCierre ?? undefined,
      montoApertura: s.saldoInicial,
      montoCierre: s.saldoRealEfectivo ?? undefined,
      saldoTeorico: s.saldoEsperadoEfectivo,
      saldoReal: s.saldoRealEfectivo ?? undefined,
      diferencia: s.diferencia ?? undefined,
      estado: s.estado,
      observaciones: s.observaciones ?? undefined,
    };
  }

  obtenerSesionActiva(): Observable<SesionCaja> {
    return this.http
      .get<BackendSesionCajaResponse>(`${this.apiUrl}/activa`)
      .pipe(map((s) => this.mapSesion(s)));
  }

  listarHistorial(): Observable<SesionCaja[]> {
    return this.http
      .get<BackendSesionCajaResponse[]>(this.apiUrl)
      .pipe(map((list) => list.map((s) => this.mapSesion(s))));
  }

  registrarMovimiento(sesionId: number, request: MovimientoCajaRequest): Observable<MovimientoCaja> {
    return this.http
      .get<BackendCategoriaFinanciera[]>(this.categoriasUrl)
      .pipe(
        switchMap((categorias) => {
          const catMatch =
            categorias.find((c) => c.tipoDefault === request.tipo) ?? categorias[0];
          const categoriaFinancieraId = catMatch ? catMatch.id : 1;

          const payload = {
            tipo: request.tipo,
            metodoPago: request.metodoPago,
            monto: request.monto,
            descripcion: request.concepto,
            referenciaExterna: request.referencia || null,
            categoriaFinancieraId,
          };

          return this.http.post<BackendMovimientoCajaResponse>(
            `${this.apiUrl}/${sesionId}/movimientos`,
            payload
          );
        }),
        map((m) => ({
          id: m.id,
          idSesionCaja: m.sesionCajaId,
          tipo: m.tipo,
          monto: m.monto,
          concepto: m.descripcion,
          metodoPago: m.metodoPago,
          referencia: m.referenciaExterna,
          fecha: m.fechaHora,
        }))
      );
  }

  abrirCaja(request: AperturaCajaRequest): Observable<SesionCaja> {
    const payload = {
      usuarioId: request.usuarioId ?? 1,
      cajero: request.cajeroNombre,
      cajeroRol: request.cajeroRol ?? 'CAJERO',
      saldoInicial: request.montoApertura,
    };
    return this.http
      .post<BackendSesionCajaResponse>(`${this.apiUrl}/abrir`, payload)
      .pipe(map((s) => this.mapSesion(s)));
  }

  cerrarCaja(id: number, request: CierreCajaRequest): Observable<SesionCaja> {
    const payload = {
      saldoRealEfectivo: request.saldoReal,
      observaciones: request.observaciones || null,
    };
    return this.http
      .post<BackendSesionCajaResponse>(`${this.apiUrl}/${id}/cerrar`, payload)
      .pipe(map((s) => this.mapSesion(s)));
  }

  obtenerResumen(): Observable<CajaReporte> {
    return this.http.get<BackendSesionCajaResponse[]>(this.apiUrl).pipe(
      map((sesiones) => {
        const totalIngresos = sesiones.reduce((acc, s) => acc + (s.totalIngresosEfectivo || 0), 0);
        const totalEgresos = sesiones.reduce((acc, s) => acc + (s.totalEgresosEfectivo || 0), 0);
        return {
          totalSesiones: sesiones.length,
          totalIngresos,
          totalEgresos,
          saldoNeto: totalIngresos - totalEgresos,
        };
      })
    );
  }
}
