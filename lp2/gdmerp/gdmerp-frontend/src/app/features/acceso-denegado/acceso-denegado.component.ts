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
      <mat-card class="acceso-denegado-card">
        <mat-card-header>
          <div mat-card-avatar class="warning-icon-avatar">
            <mat-icon color="warn">lock</mat-icon>
          </div>
          <mat-card-title>Acceso Denegado</mat-card-title>
          <mat-card-subtitle>Permisos Insuficientes (403)</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>
            No tienes los roles ni permisos necesarios para acceder a este recurso del sistema GDMERP.
          </p>
          <p>
            Si consideras que esto es un error, por favor contacta al administrador del sistema para solicitar la asignación del rol correspondiente en Keycloak.
          </p>
        </mat-card-content>
        <mat-card-actions align="end">
          <a mat-flat-button color="primary" routerLink="/home">
            <mat-icon>home</mat-icon> Volver al Inicio
          </a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .acceso-denegado-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 60vh;
      padding: 1.5rem;
    }
    .acceso-denegado-card {
      max-width: 520px;
      width: 100%;
      border-radius: 12px;
      padding: 1rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }
    .warning-icon-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .warning-icon-avatar mat-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
    }
    mat-card-title {
      font-weight: 700;
      color: #c62828;
    }
    mat-card-content p {
      margin: 0.75rem 0;
      color: #455a64;
      line-height: 1.5;
    }
  `]
})
export class AccesoDenegadoComponent {}
