import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-forgot-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="auth-page auth-page--centered">
      <div class="auth-card" style="max-width:420px; margin:auto;">
        <a routerLink="/auth/login" class="back-link">← Back to Sign In</a>

        @if (!submitted()) {
          <div class="auth-card__header">
            <h2 class="auth-card__title">Reset your password</h2>
            <p class="auth-card__subtitle">Enter your work email and we'll send you a reset link.</p>
          </div>

          <form (ngSubmit)="onSubmit()" class="auth-form">
            <div class="form-group">
              <label class="form-label" for="email">Email Address</label>
              <input id="email" type="email" class="form-control" [(ngModel)]="email"
                     name="email" required placeholder="you@company.com" />
            </div>
            <button type="submit" class="btn btn--primary w-full" [disabled]="loading()">
              @if (loading()) { <span class="spinner"></span> Sending... }
              @else { Send Reset Link }
            </button>
          </form>
        } @else {
          <div class="success-state">
            <div class="success-state__icon">✅</div>
            <h3>Check your inbox</h3>
            <p>If that email is registered, a reset link has been sent. Link expires in 1 hour.</p>
            <a routerLink="/auth/login" class="btn btn--secondary mt-4">Back to Sign In</a>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .auth-page--centered {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--surface-secondary);
      padding: 32px;
    }
    .back-link {
      display: inline-block;
      margin-bottom: 24px;
      font-size: 0.875rem;
      color: var(--color-primary-500);
    }
    .auth-form { display: flex; flex-direction: column; gap: 20px; }
    .success-state {
      text-align: center;
      padding: 16px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;

      &__icon { font-size: 3rem; }
      h3 { font-family: var(--font-display); font-size: 1.25rem; }
      p { color: var(--color-neutral-500); font-size: 0.9rem; }
    }
    .spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(0,0,0,0.2);
      border-top-color: #000;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class ForgotPasswordComponent {
  private readonly http = inject(HttpClient);
  email = '';
  loading = signal(false);
  submitted = signal(false);

  async onSubmit(): Promise<void> {
    if (!this.email) return;
    this.loading.set(true);
    try {
      await firstValueFrom(this.http.post('/api/auth/forgot-password', { email: this.email }));
    } finally {
      this.loading.set(false);
      this.submitted.set(true);
    }
  }
}
