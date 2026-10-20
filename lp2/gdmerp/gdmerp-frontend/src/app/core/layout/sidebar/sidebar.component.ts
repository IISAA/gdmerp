import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { HasRoleDirective } from '../../directives/has-role.directive';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, HasRoleDirective],
  templateUrl: './sidebar.component.html',
  styles: [`
    .active { background-color: #374151; color: white !important; font-weight: bold; }
    a:hover { background-color: #374151; }
  `]
})
export class SidebarComponent {}
