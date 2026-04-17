import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'tms-change-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="page-header"><h1 class="page-header__title">Change Password</h1></div>`
})
export class ChangePasswordComponent {}
