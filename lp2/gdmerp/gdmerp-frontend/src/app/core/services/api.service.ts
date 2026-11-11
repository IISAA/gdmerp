import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiUrl.replace(/\/api\/v1\/?$/, '');

  buildUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    if (normalizedPath.startsWith('/api/v1')) {
      return `${this.baseUrl}${normalizedPath}`;
    }
    return `${environment.apiUrl}${normalizedPath}`;
  }
}
