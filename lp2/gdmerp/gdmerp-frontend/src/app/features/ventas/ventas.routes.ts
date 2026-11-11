import { Routes } from '@angular/router';

export const VENTAS_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'reporte',
    pathMatch: 'full',
  },
  {
    path: 'nueva',
    loadComponent: () =>
      import('./venta-form/venta-form.component').then((m) => m.VentaFormComponent),
    title: 'Nueva Venta (POS) - GDM ERP',
  },
  {
    path: 'pos',
    redirectTo: 'nueva',
    pathMatch: 'full',
  },
  {
    path: 'lista',
    loadComponent: () =>
      import('./venta-list/venta-list.component').then((m) => m.VentaListComponent),
    title: 'Historial de Ventas - GDM ERP',
  },
  {
    path: 'reporte',
    loadComponent: () =>
      import('./venta-reporte/venta-reporte.component').then((m) => m.VentaReporteComponent),
    title: 'Reportes y Métricas de Ventas - GDM ERP',
  },
  {
    path: 'reportes',
    redirectTo: 'reporte',
    pathMatch: 'full',
  },
  {
    path: 'delivery',
    loadComponent: () =>
      import('./delivery/delivery-list/delivery-list.component').then((m) => m.DeliveryListComponent),
    title: 'Seguimiento de Delivery - GDM ERP',
  },
];

export const routes: Routes = VENTAS_ROUTES;
