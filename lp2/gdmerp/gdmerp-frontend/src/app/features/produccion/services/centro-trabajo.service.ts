import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CentroTrabajo } from '../models/centro-trabajo.model';

@Injectable({
  providedIn: 'root'
})
export class CentroTrabajoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/centros-trabajo`; 

  listar(): Observable<CentroTrabajo[]> {
    return this.http.get<CentroTrabajo[]>(this.apiUrl);
  }
}