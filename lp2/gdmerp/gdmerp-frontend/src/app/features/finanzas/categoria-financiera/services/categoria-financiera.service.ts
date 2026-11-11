import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { CategoriaFinanciera, TipoFinanciero } from '../models/categoria-financiera.model';

@Injectable({ providedIn: 'root' })
export class CategoriaFinancieraService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/finanzas/categorias`;

  listar(tipo?: TipoFinanciero): Observable<CategoriaFinanciera[]> {
    let params = new HttpParams();
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<CategoriaFinanciera[]>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<CategoriaFinanciera> {
    return this.http.get<CategoriaFinanciera>(`${this.apiUrl}/${id}`);
  }

  crear(categoria: CategoriaFinanciera): Observable<CategoriaFinanciera> {
    return this.http.post<CategoriaFinanciera>(this.apiUrl, categoria);
  }

  actualizar(id: number, categoria: CategoriaFinanciera): Observable<CategoriaFinanciera> {
    return this.http.put<CategoriaFinanciera>(`${this.apiUrl}/${id}`, categoria);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
