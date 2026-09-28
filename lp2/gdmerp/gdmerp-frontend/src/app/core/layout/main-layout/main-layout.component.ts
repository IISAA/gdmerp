import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  template: `
    <div style="display: flex; height: 100vh; background-color: #f3f4f6; font-family: system-ui, -apple-system, sans-serif;">
      <app-sidebar></app-sidebar>
      <div style="flex: 1; display: flex; flex-direction: column; overflow: hidden;">
        <app-header></app-header>
        <main style="flex: 1; padding: 2rem; overflow-y: auto;">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `
})
export class MainLayoutComponent {}