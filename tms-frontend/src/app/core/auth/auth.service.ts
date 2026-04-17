import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { User, LoginRequest, Role, ChangePasswordRequest } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // Signals — primary state
  private readonly _user = signal<User | null>(this.loadFromStorage());
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly activeRole = computed(() => this._user()?.activeRole ?? null);
  readonly fullName = computed(() => this._user()?.fullName ?? '');
  readonly initials = computed(() => {
    const name = this._user()?.fullName ?? '';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  });

  async login(request: LoginRequest): Promise<User> {
    const user = await firstValueFrom(
      this.http.post<User>('/api/auth/login', request, { withCredentials: true })
    );
    this._user.set(user);
    this.saveToStorage(user);
    return user;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', {}, { withCredentials: true }));
    } finally {
      this._user.set(null);
      sessionStorage.removeItem('tms_user');
      this.router.navigate(['/auth/login']);
    }
  }

  async refresh(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/refresh', {}, { withCredentials: true }));
    } catch {
      this._user.set(null);
      this.router.navigate(['/auth/login']);
    }
  }

  async changePassword(request: ChangePasswordRequest): Promise<void> {
    await firstValueFrom(
      this.http.post('/api/auth/change-password', request, { withCredentials: true })
    );
    const user = this._user();
    if (user) {
      const updated = { ...user, forcePasswordChange: false };
      this._user.set(updated);
      this.saveToStorage(updated);
    }
  }

  switchRole(role: Role): void {
    const user = this._user();
    if (user && user.roles.includes(role)) {
      const updated = { ...user, activeRole: role };
      this._user.set(updated);
      this.saveToStorage(updated);
    }
  }

  getRoleDashboardRoute(role: Role): string {
    const routes: Record<Role, string> = {
      ADMIN:    '/admin/users',
      HR:       '/hr/dashboard',
      MANAGER:  '/manager/dashboard',
      EMPLOYEE: '/employee/dashboard',
    };
    return routes[role] ?? '/employee/dashboard';
  }

  hasRole(role: Role): boolean {
    return this._user()?.roles.includes(role) ?? false;
  }

  private loadFromStorage(): User | null {
    try {
      const raw = sessionStorage.getItem('tms_user');
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  private saveToStorage(user: User): void {
    sessionStorage.setItem('tms_user', JSON.stringify(user));
  }
}
