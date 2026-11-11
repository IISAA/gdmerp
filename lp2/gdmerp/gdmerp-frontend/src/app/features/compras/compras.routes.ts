import { Routes } from '@angular/router';

export const COMPRAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./compra/compra-list/compra-list.component').then((m) => m.CompraList),
    title: 'Compras y Abastecimiento - GDM ERP',
  },
  {
    path: 'nueva',
    loadComponent: () =>
      import('./compra/compra-form/compra-form.component').then((m) => m.CompraForm),
    title: 'Nueva Orden de Compra - GDM ERP',
  },
  {
    path: 'proveedores',
    loadComponent: () =>
      import('./proveedor/proveedor-list/proveedor-list.component').then((m) => m.ProveedorList),
    title: 'Proveedores - GDM ERP',
  },
  {
    path: 'proveedores/nuevo',
    loadComponent: () =>
      import('./proveedor/proveedor-form/proveedor-form.component').then((m) => m.ProveedorForm),
    title: 'Nuevo Proveedor - GDM ERP',
  },
];

export const routes: Routes = COMPRAS_ROUTES;
