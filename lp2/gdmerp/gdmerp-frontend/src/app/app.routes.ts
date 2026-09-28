import { Routes } from '@angular/router';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout.component';

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
        path: 'produccion/ordenes', 
        loadComponent: () => import('./features/produccion/ordenes/orden-list/orden-list.component').then(m => m.OrdenListComponent) 
      },
      { 
        path: 'produccion/ordenes/nueva', 
        loadComponent: () => import('./features/produccion/ordenes/crear-orden/crear-orden.component').then(c => c.CrearOrdenComponent) 
      },
      { 
        // Nueva ruta dinámica capturando el ID
        path: 'produccion/ordenes/editar/:id', 
        loadComponent: () => import('./features/produccion/ordenes/editar-orden/editar-orden.component').then(c => c.EditarOrdenComponent) 
      }
    ]
  }
];