import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-acceso-denegado',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatCardModule],
  template: `
    <div class="acceso-denegado-container">
      <div class="acceso-denegado-card">
        <div class="card-top-icon">
          <div class="icon-circle">
            <mat-icon>lock</mat-icon>
          </div>
        </div>
        <div class="badge-tag">Seguridad & Control de Acceso</div>
        <h2 class="error-title">Acceso Restringido (403)</h2>
        <p class="error-desc">
          No cuentas con los roles ni permisos asignados en Keycloak para acceder a este módulo del sistema GDMERP.
        </p>
        <div class="help-box">
          <mat-icon class="help-icon">info</mat-icon>
          <span>Si requieres operar este módulo, por favor solicita la autorización correspondiente al Administrador de Sistemas.</span>
        </div>
        <div class="card-actions">
          <a routerLink="/dashboard" class="btn-home">
            <mat-icon>grid_view</mat-icon> Ir al Dashboard
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .acceso-denegado-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 70vh;
      padding: 2rem;
    }
    .acceso-denegado-card {
      max-width: 520px;
      width: 100%;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 2.25rem 2rem;
      box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08);
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
    }
    .card-top-icon {
      margin-bottom: 1rem;
    }
    .icon-circle {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: #fffbeb;
      color: #d97706;
      border: 2px solid #fde68a;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .icon-circle mat-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
    }
    .badge-tag {
      font-size: 0.72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #64748b;
      background: #f1f5f9;
      padding: 0.2rem 0.6rem;
      border-radius: 9999px;
      margin-bottom: 0.5rem;
    }
    .error-title {
      font-size: 1.5rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0.25rem 0 0.5rem;
    }
    .error-desc {
      color: #64748b;
      font-size: 0.925rem;
      line-height: 1.5;
      margin: 0 0 1.25rem;
    }
    .help-box {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      padding: 0.85rem 1rem;
      font-size: 0.825rem;
      color: #475569;
      text-align: left;
      margin-bottom: 1.5rem;
    }
    .help-icon {
      color: #94a3b8;
      font-size: 1.15rem;
      width: 1.15rem;
      height: 1.15rem;
      margin-top: 1px;
      flex-shrink: 0;
    }
    .card-actions {
      width: 100%;
    }
    .btn-home {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.4rem;
      width: 100%;
      padding: 0.75rem 1.25rem;
      background: #d97706;
      color: #ffffff;
      font-weight: 600;
      font-size: 0.925rem;
      border-radius: 10px;
      text-decoration: none;
      box-shadow: 0 2px 6px rgba(217, 119, 6, 0.25);
      transition: all 0.2s ease;
    }
    .btn-home:hover {
      background: #b45309;
      box-shadow: 0 4px 12px rgba(217, 119, 6, 0.35);
      transform: translateY(-1px);
    }
  `]
})
export class AccesoDenegadoComponent {}
