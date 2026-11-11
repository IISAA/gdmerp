import { Routes } from '@angular/router';

export const CAJA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./caja-dashboard/caja-dashboard.component').then((m) => m.CajaDashboard),
    title: 'Control y Arqueo de Caja - GDM ERP',
  },
];

export const routes: Routes = CAJA_ROUTES;
