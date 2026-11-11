import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  ConsultaDocumentoResponse,
  Tercero,
  TerceroCreateRequest,
  TipoTercero,
} from '../models/terceros.model';

interface BackendRucResponse {
  success?: boolean;
  message?: string;
  razonSocial?: string;
  direccion?: string;
  estado?: string;
  condicion?: string;
  distrito?: string;
  provincia?: string;
}

interface BackendDniResponse {
  success?: boolean;
  message?: string;
  nombres?: string;
  apellidoPaterno?: string;
  apellidoMaterno?: string;
  nombreCompleto?: string;
}

@Injectable({ providedIn: 'root' })
export class TercerosService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/terceros`;
  private readonly personasUrl = `${environment.apiUrl}/personas`;

  listar(tipo?: TipoTercero): Observable<Tercero[]> {
    let params = new HttpParams();
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<Tercero[]>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<Tercero> {
    return this.http.get<Tercero>(`${this.apiUrl}/${id}`);
  }

  crear(req: TerceroCreateRequest): Observable<Tercero> {
    return this.http.post<Tercero>(this.apiUrl, req);
  }

  actualizar(id: number, req: TerceroCreateRequest): Observable<Tercero> {
    return this.http.put<Tercero>(`${this.apiUrl}/${id}`, req);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  consultarDocumento(tipoDocumento: string, numeroDocumento: string): Observable<ConsultaDocumentoResponse> {
    const tipo = tipoDocumento.trim().toUpperCase();
    const numero = numeroDocumento.trim();

    if (tipo === 'RUC') {
      return this.http
        .get<BackendRucResponse>(`${this.personasUrl}/ruc/${numero}`)
        .pipe(
          map((res: BackendRucResponse) => {
            const razonSocial = res.razonSocial?.trim() ?? '';
            return {
              exito: Boolean(res.success ?? razonSocial.length > 0),
              mensaje: res.message,
              razonSocial,
              direccion: res.direccion,
              estado: res.estado,
              condicion: res.condicion,
              distrito: res.distrito,
              provincia: res.provincia,
            };
          })
        );
    }

    if (tipo === 'DNI') {
      return this.http
        .get<BackendDniResponse>(`${this.personasUrl}/dni/${numero}`)
        .pipe(
          map((res: BackendDniResponse) => {
            const nombre =
              res.nombreCompleto?.trim() ||
              [res.nombres, res.apellidoPaterno, res.apellidoMaterno]
                .filter((p): p is string => Boolean(p && p.trim()))
                .join(' ')
                .trim();
            return {
              exito: Boolean(res.success ?? nombre.length > 0),
              mensaje: res.message,
              razonSocial: nombre,
              estado: 'VALIDADO',
              condicion: 'RENIEC',
            };
          })
        );
    }

    return of({
      exito: false,
      mensaje: 'La consulta automática en línea solo está disponible para RUC o DNI.',
      razonSocial: '',
    });
  }
}
