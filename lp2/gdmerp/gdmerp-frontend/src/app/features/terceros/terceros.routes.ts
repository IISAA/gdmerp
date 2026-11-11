import { Routes } from '@angular/router';
import { AuthGuard } from '../../core/guards/auth.guard';

export const TERCEROS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./terceros-list/terceros-list.component').then((m) => m.TercerosList),
    title: 'Directorio de Terceros (CRM) - GDM ERP',
  },
  {
    path: 'nuevo',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'VENDEDOR', 'SUPERVISOR'] },
    loadComponent: () =>
      import('./terceros-form/terceros-form.component').then((m) => m.TercerosForm),
    title: 'Registrar Tercero - GDM ERP',
  },
  {
    path: ':id/editar',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'VENDEDOR', 'SUPERVISOR'] },
    loadComponent: () =>
      import('./terceros-form/terceros-form.component').then((m) => m.TercerosForm),
    title: 'Editar Tercero - GDM ERP',
  },
];

export const routes: Routes = TERCEROS_ROUTES;
