import { Routes } from '@angular/router';
import { AuthGuard } from '../../core/guards/auth.guard';

export const CATALOGO_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'productos',
    pathMatch: 'full',
  },
  {
    path: 'productos',
    loadComponent: () =>
      import('./producto/producto-list/producto-list.component').then((m) => m.ProductoList),
    title: 'Catálogo de Productos - GDM ERP',
  },
  {
    path: 'productos/nuevo',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./producto/producto-form/producto-form.component').then((m) => m.ProductoForm),
    title: 'Nuevo Producto - GDM ERP',
  },
  {
    path: 'productos/:id/editar',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./producto/producto-form/producto-form.component').then((m) => m.ProductoForm),
    title: 'Editar Producto - GDM ERP',
  },
  {
    path: 'categorias',
    loadComponent: () =>
      import('./categoria/categoria-list/categoria-list.component').then((m) => m.CategoriaList),
    title: 'Categorías de Catálogo - GDM ERP',
  },
  {
    path: 'categorias/nueva',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./categoria/categoria-form/categoria-form.component').then((m) => m.CategoriaForm),
    title: 'Nueva Categoría - GDM ERP',
  },
  {
    path: 'categorias/:id',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./categoria/categoria-form/categoria-form.component').then((m) => m.CategoriaForm),
    title: 'Editar Categoría - GDM ERP',
  },
];

export const routes: Routes = CATALOGO_ROUTES;
