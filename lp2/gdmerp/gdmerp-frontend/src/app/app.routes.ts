import { Routes } from '@angular/router';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout.component';
import { AuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      { 
        path: 'home', 
        loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) 
      },
      { 
        path: 'acceso-denegado', 
        loadComponent: () => import('./features/acceso-denegado/acceso-denegado.component').then(m => m.AccesoDenegadoComponent) 
      },
      { 
        path: 'produccion/ordenes', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO'] },
        loadComponent: () => import('./features/produccion/ordenes/orden-list/orden-list.component').then(m => m.OrdenListComponent) 
      },
      { 
        path: 'produccion/ordenes/nueva', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'JEFE_PRODUCCION'] },
        loadComponent: () => import('./features/produccion/ordenes/crear-orden/crear-orden.component').then(c => c.CrearOrdenComponent) 
      },
      { 
        path: 'produccion/ordenes/editar/:id', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'JEFE_PRODUCCION', 'OPERARIO'] },
        loadComponent: () => import('./features/produccion/ordenes/editar-orden/editar-orden.component').then(c => c.EditarOrdenComponent) 
      },
      { 
        path: 'inventario/productos', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO'] },
        loadComponent: () => import('./features/inventario/productos/lista-productos/lista-productos.component').then(m => m.ListaProductosComponent) 
      },
      { 
        path: 'inventario/productos/nuevo', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'ALMACENERO'] },
        loadComponent: () => import('./features/inventario/productos/formulario-producto/formulario-producto.component').then(m => m.FormularioProductoComponent) 
      },
      { 
        path: 'inventario/productos/editar/:id', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'ALMACENERO'] },
        loadComponent: () => import('./features/inventario/productos/formulario-producto/formulario-producto.component').then(m => m.FormularioProductoComponent) 
      },
      { 
        path: 'inventario', 
        redirectTo: 'inventario/productos', 
        pathMatch: 'full' 
      }
    ]
  },
  { path: '**', redirectTo: 'home' }
];
