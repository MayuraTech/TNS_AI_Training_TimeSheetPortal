import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'tms-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">👤 My Profile</h1>
        <p class="page-header__subtitle">Manage your account settings</p>
      </div>
    </div>

    <div class="profile-grid">

      <!-- Profile Card -->
      <div class="card">
        <div class="profile-avatar-section">
          <div class="profile-avatar">{{ initials() }}</div>
          <div>
            <h2 class="profile-name">{{ user()?.fullName }}</h2>
            <p class="profile-email">{{ user()?.email }}</p>
            <div class="profile-roles">
              @for (role of user()?.roles; track role) {
                <span class="badge badge--pending" style="font-size:0.75rem">{{ role }}</span>
              }
            </div>
          </div>
        </div>

        <div class="divider"></div>

        <div class="profile-info-grid">
          <div class="profile-info-item">
            <span class="profile-info-item__label">Timezone</span>
            <span class="profile-info-item__value">{{ user()?.timezone ?? 'UTC' }}</span>
          </div>
          <div class="profile-info-item">
            <span class="profile-info-item__label">Active Role</span>
            <span class="profile-info-item__value">{{ user()?.activeRole }}</span>
          </div>
        </div>
      </div>

      <!-- Change Password Card -->
      <div class="card">
        <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:20px">🔑 Change Password</h3>

        @if (pwSuccess()) {
          <div class="alert-success mb-4">✅ Password changed successfully!</div>
        }
        @if (pwError()) {
          <div class="alert-error mb-4">⚠️ {{ pwError() }}</div>
        }

        <form (ngSubmit)="changePassword()" class="flex flex-col gap-4">
          <div class="form-group">
            <label class="form-label">Current Password</label>
            <div style="position:relative">
              <input
                [type]="showCurrent() ? 'text' : 'password'"
                class="form-control"
                [(ngModel)]="pw.current"
                name="current"
                placeholder="Enter current password"
                style="padding-right:48px"
              />
              <button type="button" class="eye-btn" (click)="showCurrent.set(!showCurrent())">
                {{ showCurrent() ? '🙈' : '👁' }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">New Password</label>
            <div style="position:relative">
              <input
                [type]="showNew() ? 'text' : 'password'"
                class="form-control"
                [(ngModel)]="pw.newPw"
                name="newPw"
                placeholder="Min 8 chars, uppercase, number, special char"
                style="padding-right:48px"
              />
              <button type="button" class="eye-btn" (click)="showNew.set(!showNew())">
                {{ showNew() ? '🙈' : '👁' }}
              </button>
            </div>
            <!-- Password strength -->
            @if (pw.newPw) {
              <div class="pw-strength">
                <div class="pw-strength__bar">
                  <div class="pw-strength__fill" [style.width.%]="pwStrength()" [class]="pwStrengthClass()"></div>
                </div>
                <span class="pw-strength__label" [class]="pwStrengthClass()">{{ pwStrengthLabel() }}</span>
              </div>
            }
          </div>

          <div class="form-group">
            <label class="form-label">Confirm New Password</label>
            <input
              type="password"
              class="form-control"
              [(ngModel)]="pw.confirm"
              name="confirm"
              placeholder="Repeat new password"
              [class.form-control--error]="pw.confirm && pw.newPw !== pw.confirm"
            />
            @if (pw.confirm && pw.newPw !== pw.confirm) {
              <span class="form-error">⚠ Passwords do not match</span>
            }
          </div>

          <button
            type="submit"
            class="btn btn--primary"
            [disabled]="saving() || !pw.current || !pw.newPw || pw.newPw !== pw.confirm"
          >
            @if (saving()) { <span class="spinner-sm"></span> Saving... }
            @else { Update Password }
          </button>
        </form>
      </div>

    </div>
  `,
  styles: [`
    .profile-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      @media (max-width: 768px) { grid-template-columns: 1fr; }
    }

    .profile-avatar-section {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 20px;
    }

    .profile-avatar {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: var(--font-display);
      font-size: 1.5rem;
      font-weight: 700;
      color: #fff;
      flex-shrink: 0;
      box-shadow: var(--shadow-md);
    }

    .profile-name {
      font-family: var(--font-display);
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--color-neutral-900);
      margin-bottom: 4px;
    }

    .profile-email {
      font-size: 0.875rem;
      color: var(--color-neutral-500);
      margin-bottom: 8px;
    }

    .profile-roles { display: flex; gap: 6px; flex-wrap: wrap; }

    .profile-info-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .profile-info-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 12px;
      background: var(--surface-secondary);
      border-radius: var(--border-radius-md);

      &__label { font-size: 0.75rem; color: var(--color-neutral-500); text-transform: uppercase; letter-spacing: 0.06em; font-weight: 600; }
      &__value { font-size: 0.9375rem; font-weight: 600; color: var(--color-neutral-800); }
    }

    .eye-btn {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      font-size: 1rem;
      opacity: 0.6;
      &:hover { opacity: 1; }
    }

    .pw-strength {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-top: 6px;

      &__bar {
        flex: 1;
        height: 4px;
        background: var(--color-neutral-200);
        border-radius: 2px;
        overflow: hidden;
      }

      &__fill {
        height: 100%;
        border-radius: 2px;
        transition: width 0.3s ease, background 0.3s ease;
        &.weak   { background: var(--color-danger); }
        &.fair   { background: var(--color-warning); }
        &.good   { background: var(--color-accent-500); }
        &.strong { background: var(--color-success); }
      }

      &__label {
        font-size: 0.75rem;
        font-weight: 600;
        min-width: 48px;
        &.weak   { color: var(--color-danger); }
        &.fair   { color: var(--color-warning); }
        &.good   { color: var(--color-accent-600); }
        &.strong { color: var(--color-success); }
      }
    }

    .alert-success {
      padding: 10px 14px;
      background: var(--color-success-light);
      color: var(--color-success);
      border-radius: var(--border-radius-md);
      font-size: 0.875rem;
      font-weight: 500;
    }

    .alert-error {
      padding: 10px 14px;
      background: var(--color-danger-light);
      color: var(--color-danger);
      border-radius: var(--border-radius-md);
      font-size: 0.875rem;
      font-weight: 500;
    }

    .spinner-sm {
      width: 14px; height: 14px;
      border: 2px solid rgba(0,0,0,0.2);
      border-top-color: #000;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class ProfileComponent {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  readonly user = this.authService.user;
  readonly initials = this.authService.initials;

  pw = { current: '', newPw: '', confirm: '' };
  saving = signal(false);
  pwSuccess = signal(false);
  pwError = signal<string | null>(null);
  showCurrent = signal(false);
  showNew = signal(false);

  pwStrength(): number {
    const p = this.pw.newPw;
    if (!p) return 0;
    let score = 0;
    if (p.length >= 8)  score += 25;
    if (p.length >= 12) score += 15;
    if (/[A-Z]/.test(p)) score += 20;
    if (/[0-9]/.test(p)) score += 20;
    if (/[^a-zA-Z0-9]/.test(p)) score += 20;
    return Math.min(score, 100);
  }

  pwStrengthClass(): string {
    const s = this.pwStrength();
    if (s < 30) return 'weak';
    if (s < 60) return 'fair';
    if (s < 80) return 'good';
    return 'strong';
  }

  pwStrengthLabel(): string {
    const labels: Record<string, string> = { weak: 'Weak', fair: 'Fair', good: 'Good', strong: 'Strong' };
    return labels[this.pwStrengthClass()];
  }

  async changePassword(): Promise<void> {
    if (!this.pw.current || !this.pw.newPw || this.pw.newPw !== this.pw.confirm) return;

    this.saving.set(true);
    this.pwError.set(null);
    this.pwSuccess.set(false);

    try {
      await firstValueFrom(this.http.post('/api/auth/change-password', {
        currentPassword: this.pw.current,
        newPassword: this.pw.newPw
      }, { withCredentials: true }));

      this.pwSuccess.set(true);
      this.pw = { current: '', newPw: '', confirm: '' };
      setTimeout(() => this.pwSuccess.set(false), 4000);
    } catch (err: any) {
      this.pwError.set(err?.error?.error ?? 'Failed to change password. Check your current password.');
    } finally {
      this.saving.set(false);
    }
  }
}
