import { Routes } from '@angular/router';

export const FINANZAS_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'reportes',
    pathMatch: 'full',
  },
  {
    path: 'reportes',
    loadComponent: () =>
      import('./reporte-financiero/reporte-financiero-list/reporte-financiero-list.component').then(
        (m) => m.ReporteFinancieroList
      ),
    title: 'Reportes y Métricas Financieras - GDM ERP',
  },
  {
    path: 'reporte',
    redirectTo: 'reportes',
    pathMatch: 'full',
  },
  {
    path: 'sesiones',
    loadComponent: () =>
      import('./sesion-caja/sesion-caja-list/sesion-caja-list.component').then(
        (m) => m.SesionCajaList
      ),
    title: 'Sesiones de Caja - GDM ERP',
  },
  {
    path: 'sesiones/nueva',
    loadComponent: () =>
      import('./sesion-caja/sesion-caja-form/sesion-caja-form.component').then(
        (m) => m.SesionCajaForm
      ),
    title: 'Apertura de Caja - GDM ERP',
  },
  {
    path: 'sesiones/:id',
    loadComponent: () =>
      import('./sesion-caja/sesion-caja-form/sesion-caja-form.component').then(
        (m) => m.SesionCajaForm
      ),
    title: 'Detalle de Sesión de Caja - GDM ERP',
  },
  {
    path: 'rendiciones',
    loadComponent: () =>
      import('./rendicion-caja-chica/rendicion-caja-chica-list/rendicion-caja-chica-list.component').then(
        (m) => m.RendicionCajaChicaList
      ),
    title: 'Rendiciones de Caja Chica - GDM ERP',
  },
  {
    path: 'rendiciones/nueva',
    loadComponent: () =>
      import('./rendicion-caja-chica/rendicion-caja-chica-form/rendicion-caja-chica-form.component').then(
        (m) => m.RendicionCajaChicaForm
      ),
    title: 'Nueva Rendición de Caja Chica - GDM ERP',
  },
  {
    path: 'creditos',
    loadComponent: () =>
      import('./cuenta-cobrar/cuenta-cobrar-list/cuenta-cobrar-list.component').then(
        (m) => m.CuentaCobrarList
      ),
    title: 'Cuentas por Cobrar y Créditos - GDM ERP',
  },
  {
    path: 'creditos/nuevo',
    loadComponent: () =>
      import('./cuenta-cobrar/cuenta-cobrar-form/cuenta-cobrar-form.component').then(
        (m) => m.CuentaCobrarForm
      ),
    title: 'Nuevo Crédito Comercial - GDM ERP',
  },
  {
    path: 'creditos/:id',
    loadComponent: () =>
      import('./cuenta-cobrar/cuenta-cobrar-form/cuenta-cobrar-form.component').then(
        (m) => m.CuentaCobrarForm
      ),
    title: 'Detalle de Crédito - GDM ERP',
  },
  {
    path: 'transacciones',
    loadComponent: () =>
      import('./transaccion-financiera/transaccion-financiera-list/transaccion-financiera-list.component').then(
        (m) => m.TransaccionFinancieraList
      ),
    title: 'Transacciones Financieras - GDM ERP',
  },
  {
    path: 'transacciones/nueva',
    loadComponent: () =>
      import('./transaccion-financiera/transaccion-financiera-form/transaccion-financiera-form.component').then(
        (m) => m.TransaccionFinancieraForm
      ),
    title: 'Nueva Transacción Financiera - GDM ERP',
  },
  {
    path: 'transacciones/:id/editar',
    loadComponent: () =>
      import('./transaccion-financiera/transaccion-financiera-form/transaccion-financiera-form.component').then(
        (m) => m.TransaccionFinancieraForm
      ),
    title: 'Editar Transacción Financiera - GDM ERP',
  },
  {
    path: 'categorias',
    loadComponent: () =>
      import('./categoria-financiera/categoria-financiera-list/categoria-financiera-list.component').then(
        (m) => m.CategoriaFinancieraList
      ),
    title: 'Categorías Financieras - GDM ERP',
  },
  {
    path: 'categorias/nueva',
    loadComponent: () =>
      import('./categoria-financiera/categoria-financiera-form/categoria-financiera-form.component').then(
        (m) => m.CategoriaFinancieraForm
      ),
    title: 'Nueva Categoría Financiera - GDM ERP',
  },
  {
    path: 'categorias/:id/editar',
    loadComponent: () =>
      import('./categoria-financiera/categoria-financiera-form/categoria-financiera-form.component').then(
        (m) => m.CategoriaFinancieraForm
      ),
    title: 'Editar Categoría Financiera - GDM ERP',
  },
];

export const routes: Routes = FINANZAS_ROUTES;
