import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'tms-reset-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="page-header"><h1 class="page-header__title">Reset Password</h1></div>`
})
export class ResetPasswordComponent {}
