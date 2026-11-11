import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  DashboardFinanciero,
  FlujoCajaResumen,
  ReporteOmnicanal,
  ReporteRendicionesAgregado,
  ReporteSemaforoInventario,
} from '../models/reporte-financiero.model';

@Injectable({ providedIn: 'root' })
export class ReporteFinancieroService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/finanzas/reportes`;

  obtenerDashboard(): Observable<DashboardFinanciero> {
    return this.http.get<DashboardFinanciero>(`${this.baseUrl}/dashboard`);
  }

  obtenerFlujoCaja(): Observable<FlujoCajaResumen> {
    return this.http.get<FlujoCajaResumen>(`${this.baseUrl}/flujo-caja`);
  }

  obtenerRendicionesAgregado(): Observable<ReporteRendicionesAgregado> {
    return this.http.get<ReporteRendicionesAgregado>(`${this.baseUrl}/rendiciones-agregado`);
  }

  descargarCierreCaja(sesionId: number, formato: 'PDF' | 'EXCEL'): Observable<Blob> {
    const params = new HttpParams().set('formato', formato);
    return this.http.get(`${this.baseUrl}/cierre-caja/${sesionId}/exportar`, {
      params,
      responseType: 'blob',
    });
  }

  obtenerReporteOmnicanal(): Observable<ReporteOmnicanal> {
    return this.http.get<ReporteOmnicanal>(`${environment.apiUrl}/ventas/reporte-omnicanal`);
  }

  obtenerReporteSemaforo(): Observable<ReporteSemaforoInventario> {
    return this.http.get<ReporteSemaforoInventario>(`${environment.apiUrl}/inventario/reporte-semaforo`);
  }
}
