import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  standalone: true,
  template: `
    <header style="display: flex; flex-direction: column; width: 100%;">
      <div style="background-color: #f59e0b; color: white; text-align: center; padding: 0.5rem; font-weight: bold; font-size: 0.875rem;">
        🚀 ¡Nuevos despachos de Pasta Pura de Cacao y Miel disponibles para producción inmediata!
      </div>
      <div style="display: flex; justify-content: space-between; align-items: center; padding: 1rem 2rem; background: white; border-bottom: 1px solid #e5e7eb;">
        <input type="text" placeholder="Buscar órdenes, insumos o lotes..." style="padding: 0.5rem 1rem; border: 1px solid #d1d5db; border-radius: 9999px; width: 350px; outline: none;">
        <div style="display: flex; align-items: center; gap: 1rem;">
          <span style="font-weight: 600; color: #374151;">Jefe de Producción</span>
          <div style="width: 35px; height: 35px; background: #3b82f6; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold;">
            JP
          </div>
        </div>
      </div>
    </header>
  `
})
export class HeaderComponent {}