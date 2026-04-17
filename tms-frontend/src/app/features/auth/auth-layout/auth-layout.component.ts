import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'tms-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AuthLayoutComponent {}
