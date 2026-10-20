import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  readonly authService = inject(AuthService);

  getInitials(): string {
    const user = this.authService.getUsername() || 'G';
    return user.substring(0, 2).toUpperCase();
  }
}
