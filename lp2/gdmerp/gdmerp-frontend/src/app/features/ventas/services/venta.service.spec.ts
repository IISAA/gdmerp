import '@angular/compiler';
import { describe, it, expect, vi } from 'vitest';
import { Injector } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { VentaService } from './venta.service';
import { of } from 'rxjs';
import { environment } from '../../../../environments/environment';

describe('VentaService', () => {
  it('debe enviar los parametros correctos en reporte() incluyendo metodoPago', () => {
    const mockHttp = {
      get: vi.fn().mockReturnValue(of({ totalVentas: 10, totalAnuladas: 2 })),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    };

    const injector = Injector.create({
      providers: [
        { provide: HttpClient, useValue: mockHttp },
        VentaService,
      ],
    });

    const service = injector.get(VentaService);

    service.reporte('REGISTRADA', 'YAPE', '2026-11-01T00:00:00', '2026-11-07T23:59:59').subscribe((res: any) => {
      expect(res.totalVentas).toBe(10);
    });

    expect(mockHttp.get).toHaveBeenCalled();
    const [url, options] = mockHttp.get.mock.calls[0];
    expect(url).toBe(`${environment.apiUrl}/ventas/resumen`);
    expect(options.params.get('estado')).toBe('REGISTRADA');
    expect(options.params.get('metodoPago')).toBe('YAPE');
    expect(options.params.get('desde')).toBe('2026-11-01T00:00:00');
    expect(options.params.get('hasta')).toBe('2026-11-07T23:59:59');
  });

  it('debe enviar los parametros correctos en buscarPaginado()', () => {
    const mockHttp = {
      get: vi.fn().mockReturnValue(of({ content: [], totalElements: 0, totalPages: 0 })),
    };

    const injector = Injector.create({
      providers: [
        { provide: HttpClient, useValue: mockHttp },
        VentaService,
      ],
    });

    const service = injector.get(VentaService);

    service.buscarPaginado('REGISTRADA', 'EFECTIVO', undefined, undefined, 1, 15).subscribe((res: any) => {
      expect(res.totalElements).toBe(0);
    });

    expect(mockHttp.get).toHaveBeenCalled();
    const [url, options] = mockHttp.get.mock.calls[0];
    expect(url).toBe(`${environment.apiUrl}/ventas`);
    expect(options.params.get('metodoPago')).toBe('EFECTIVO');
    expect(options.params.get('page')).toBe('1');
    expect(options.params.get('size')).toBe('15');
  });
});
