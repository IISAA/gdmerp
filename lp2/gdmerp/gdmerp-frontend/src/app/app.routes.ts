import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { InventarioComponent } from './features/inventario/inventario.component';
import { ProductoListComponent } from './features/inventario/productos/lista-productos/lista-productos.component';
import { AlmacenListComponent } from './features/inventario/almacenes/almacen-list/almacen-list.component';
import { CategoriaListComponent } from './features/inventario/categorias/categoria-list/categoria-list.component';
import { ProduccionLayoutComponent } from './features/produccion/produccion-layout/produccion-layout.component';
import { OrdenListComponent } from './features/produccion/ordenes/orden-list/orden-list.component';
import { OrdenesListaComponent } from './features/produccion/ordenes/ordenes-lista/ordenes-lista.component';
import { OrdenDetalleComponent } from './features/produccion/ordenes/orden-detalle/orden-detalle.component';
import { CentroTrabajoListComponent } from './features/produccion/centros-trabajo/centro-trabajo-list/centro-trabajo-list.component';
import { ListaMaterialesListComponent } from './features/produccion/lista-materiales/lista-materiales-list/lista-materiales-list.component';
import { ListaMaterialesFormComponent } from './features/produccion/lista-materiales/lista-materiales-form/lista-materiales-form.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO'] } // Vista común pero adaptada con *hasRole si fuera necesario
  },
  {
    path: 'inventario',
    component: InventarioComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'ALMACENERO'] },
    children: [
      { path: '', redirectTo: 'productos', pathMatch: 'full' },
      { path: 'productos', component: ProductoListComponent },
      { 
        path: 'productos/nuevo', 
        loadComponent: () => import('./features/inventario/productos/formulario-producto/formulario-producto.component').then(m => m.FormularioProductoComponent) 
      },
      { 
        path: 'productos/editar/:id', 
        loadComponent: () => import('./features/inventario/productos/formulario-producto/formulario-producto.component').then(m => m.FormularioProductoComponent) 
      },
      { path: 'almacenes', component: AlmacenListComponent },
      { path: 'categorias', component: CategoriaListComponent },
      { 
        path: 'unidades-medida', 
        loadComponent: () => import('./features/inventario/unidades-medida/unidades-medida.component').then(m => m.UnidadesMedidaComponent) 
      }
    ]
  },
  {
    path: 'produccion',
    component: ProduccionLayoutComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'JEFE_PRODUCCION', 'OPERARIO'] },
    children: [
      { path: '', redirectTo: 'ordenes', pathMatch: 'full' },
      { path: 'ordenes', component: OrdenListComponent },
      { path: 'ordenes/lista', component: OrdenesListaComponent },
      { 
        path: 'ordenes/nueva', 
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'JEFE_PRODUCCION'] },
        loadComponent: () => import('./features/produccion/ordenes/crear-orden/crear-orden.component').then(c => c.CrearOrdenComponent) 
      },
      { path: 'ordenes/:id', component: OrdenDetalleComponent },
      { path: 'centros-trabajo', component: CentroTrabajoListComponent },
      { path: 'listas-materiales', component: ListaMaterialesListComponent },
      { path: 'listas-materiales/nueva', component: ListaMaterialesFormComponent },
      { path: 'listas-materiales/:id', component: ListaMaterialesFormComponent },
      { path: 'lista-materiales', redirectTo: 'listas-materiales', pathMatch: 'full' }
    ]
  },
  {
    path: 'reportes',
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'stock', pathMatch: 'full' },
      { 
        path: 'stock', 
        data: { roles: ['ADMIN', 'ALMACENERO'] },
        loadComponent: () => import('./features/reportes/reporte-stock/reporte-stock.component').then(m => m.ReporteStockComponent) 
      },
      { 
        path: 'stock-critico', 
        data: { roles: ['ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION'] },
        loadComponent: () => import('./features/reportes/reporte-stock-critico/reporte-stock-critico.component').then(m => m.ReporteStockCriticoComponent) 
      },
      { 
        path: 'ordenes-estados', 
        data: { roles: ['ADMIN', 'JEFE_PRODUCCION'] },
        loadComponent: () => import('./features/reportes/reporte-ordenes/reporte-ordenes.component').then(m => m.ReporteOrdenesComponent) 
      }
    ]
  },
  {
    path: 'acceso-denegado',
    loadComponent: () => import('./features/acceso-denegado/acceso-denegado.component').then(m => m.AccesoDenegadoComponent)
  },
  {
    path: 'home',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
