import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'tms-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="auth-page">
      <!-- Left panel — branding -->
      <div class="auth-page__brand">
        <div class="brand-content">
          <div class="brand-logo">
            <span class="brand-logo__mark">T</span>
            <span class="brand-logo__text">TMS</span>
          </div>
          <h1 class="brand-headline">Track time.<br>Stay aligned.</h1>
          <p class="brand-sub">
            The modern timesheet platform for teams that value transparency and accountability.
          </p>
          <div class="brand-stats">
            <div class="brand-stat">
              <span class="brand-stat__value">99.9%</span>
              <span class="brand-stat__label">Uptime</span>
            </div>
            <div class="brand-stat">
              <span class="brand-stat__value">2s</span>
              <span class="brand-stat__label">Avg load</span>
            </div>
            <div class="brand-stat">
              <span class="brand-stat__value">SOC2</span>
              <span class="brand-stat__label">Compliant</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right panel — form -->
      <div class="auth-page__form-panel">
        <div class="auth-card">
          <div class="auth-card__header">
            <h2 class="auth-card__title">Welcome back</h2>
            <p class="auth-card__subtitle">Sign in to your TMS account</p>
          </div>

          <!-- Error message -->
          @if (errorMsg()) {
            <div class="auth-error" role="alert">
              <span class="auth-error__icon">⚠</span>
              <span>{{ errorMsg() }}</span>
            </div>
          }

          <form class="auth-form" (ngSubmit)="onSubmit()" #loginForm="ngForm">
            <!-- Email -->
            <div class="form-group">
              <label class="form-label" for="email">Email Address</label>
              <input
                id="email"
                type="email"
                class="form-control"
                placeholder="you@company.com"
                [(ngModel)]="email"
                name="email"
                required
                email
                autocomplete="email"
                [class.form-control--error]="submitted && !email"
              />
            </div>

            <!-- Password -->
            <div class="form-group">
              <label class="form-label" for="password">Password</label>
              <div class="input-with-action">
                <input
                  id="password"
                  [type]="showPassword() ? 'text' : 'password'"
                  class="form-control"
                  placeholder="••••••••"
                  [(ngModel)]="password"
                  name="password"
                  required
                  autocomplete="current-password"
                  [class.form-control--error]="submitted && !password"
                />
                <button
                  type="button"
                  class="input-action-btn"
                  (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'"
                >
                  {{ showPassword() ? '🙈' : '👁' }}
                </button>
              </div>
            </div>

            <!-- Submit -->
            <button
              type="submit"
              class="btn btn--primary w-full"
              [disabled]="loading()"
            >
              @if (loading()) {
                <span class="spinner"></span>
                Signing in...
              } @else {
                Sign In
              }
            </button>
          </form>

          <div class="auth-card__footer">
            <a routerLink="/auth/forgot-password" class="auth-link">
              Forgot your password?
            </a>
          </div>
        </div>

        <p class="auth-page__copyright">© 2025 Think N Solutions</p>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      display: grid;
      grid-template-columns: 1fr 1fr;
      min-height: 100vh;

      @media (max-width: 768px) {
        grid-template-columns: 1fr;
      }
    }

    .auth-page__brand {
      background: linear-gradient(135deg, var(--color-primary-900) 0%, var(--color-primary-700) 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 48px;
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        width: 600px;
        height: 600px;
        border-radius: 50%;
        background: radial-gradient(circle, rgba(245,158,11,0.08) 0%, transparent 70%);
        top: -100px;
        right: -100px;
      }

      @media (max-width: 768px) { display: none; }
    }

    .brand-content { position: relative; z-index: 1; max-width: 400px; }

    .brand-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 40px;

      &__mark {
        width: 44px;
        height: 44px;
        background: var(--color-accent-500);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 800;
        font-size: 1.25rem;
        color: var(--color-neutral-900);
      }

      &__text {
        font-family: var(--font-display);
        font-size: 1.5rem;
        font-weight: 700;
        color: #fff;
      }
    }

    .brand-headline {
      font-family: var(--font-display);
      font-size: 2.75rem;
      font-weight: 700;
      color: #fff;
      line-height: 1.15;
      margin-bottom: 20px;
    }

    .brand-sub {
      font-size: 1rem;
      color: rgba(255,255,255,0.65);
      line-height: 1.7;
      margin-bottom: 48px;
    }

    .brand-stats {
      display: flex;
      gap: 32px;
    }

    .brand-stat {
      display: flex;
      flex-direction: column;
      gap: 4px;

      &__value {
        font-family: var(--font-display);
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--color-accent-400);
      }

      &__label {
        font-size: 0.8125rem;
        color: rgba(255,255,255,0.5);
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }
    }

    .auth-page__form-panel {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px 32px;
      background: var(--surface-secondary);
    }

    .auth-card {
      width: 100%;
      max-width: 420px;
      background: var(--surface-elevated);
      border-radius: var(--border-radius-xl);
      box-shadow: var(--shadow-xl);
      padding: 40px;
      animation: slideUp var(--transition-slow) ease-out;

      &__header { margin-bottom: 28px; }

      &__title {
        font-family: var(--font-display);
        font-size: 1.75rem;
        font-weight: 700;
        color: var(--color-neutral-900);
        margin-bottom: 6px;
      }

      &__subtitle {
        font-size: 0.9375rem;
        color: var(--color-neutral-500);
      }

      &__footer {
        margin-top: 20px;
        text-align: center;
      }
    }

    .auth-error {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: var(--color-danger-light);
      border: 1px solid rgba(220,38,38,0.2);
      border-radius: var(--border-radius-md);
      color: var(--color-danger);
      font-size: 0.875rem;
      margin-bottom: 20px;

      &__icon { font-size: 1rem; }
    }

    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .input-with-action {
      position: relative;

      .form-control { padding-right: 48px; }
    }

    .input-action-btn {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      font-size: 1rem;
      padding: 4px;
      opacity: 0.6;
      transition: opacity var(--transition-fast);
      &:hover { opacity: 1; }
    }

    .auth-link {
      font-size: 0.875rem;
      color: var(--color-primary-500);
      text-decoration: none;
      transition: color var(--transition-fast);
      &:hover { color: var(--color-primary-400); }
    }

    .auth-page__copyright {
      margin-top: 32px;
      font-size: 0.8125rem;
      color: var(--color-neutral-400);
    }

    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(0,0,0,0.2);
      border-top-color: var(--color-neutral-900);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to   { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  submitted = false;

  loading = signal(false);
  errorMsg = signal<string | null>(null);
  showPassword = signal(false);

  async onSubmit(): Promise<void> {
    this.submitted = true;
    if (!this.email || !this.password) return;

    this.loading.set(true);
    this.errorMsg.set(null);

    try {
      const user = await this.authService.login({ email: this.email, password: this.password });

      if (user.forcePasswordChange) {
        this.router.navigate(['/auth/change-password']);
      } else {
        this.router.navigate([this.authService.getRoleDashboardRoute(user.activeRole)]);
      }
    } catch (err: any) {
      if (err?.status === 423) {
        const until = err.error?.lockedUntil
          ? new Date(err.error.lockedUntil).toLocaleTimeString()
          : 'a few minutes';
        this.errorMsg.set(`Account locked. Try again after ${until}.`);
      } else {
        this.errorMsg.set('Invalid email or password.');
      }
    } finally {
      this.loading.set(false);
    }
  }
}
