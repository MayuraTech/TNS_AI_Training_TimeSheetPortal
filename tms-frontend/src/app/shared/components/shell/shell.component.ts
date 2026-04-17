import {
  Component, inject, signal, computed, ChangeDetectionStrategy, OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { Role } from '../../../core/models/user.model';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

interface NavSection {
  title: string;
  items: NavItem[];
  roles: Role[];
}

@Component({
  selector: 'tms-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell" [class.sidebar-collapsed]="sidebarCollapsed()">

      <!-- ── Top Header ── -->
      <header class="app-header">
        <div class="app-header__left">
          <button class="app-header__toggle" (click)="toggleSidebar()" aria-label="Toggle sidebar">
            <span>☰</span>
          </button>
          <a routerLink="/" class="app-header__logo">
            <span class="app-header__logo-mark">T</span>
            @if (!sidebarCollapsed()) {
              <span class="app-header__logo-text">TMS</span>
            }
          </a>
        </div>

        <div class="app-header__search">
          <span class="app-header__search-icon">🔍</span>
          <input type="text" placeholder="Search entries, projects, people..." />
        </div>

        <div class="app-header__right">
          <!-- Notifications bell -->
          <button class="app-header__icon-btn" aria-label="Notifications">
            <span>🔔</span>
            <span class="badge-count">3</span>
          </button>

          <!-- Role switcher — only show if multiple roles -->
          @if (availableRoles().length > 1) {
            <div class="role-switcher-wrap" (click)="showRoleSwitcher.set(!showRoleSwitcher())">
              <div class="app-header__role-switcher">
                <span class="role-badge">{{ activeRole() }}</span>
                <span style="color:rgba(255,255,255,0.6);font-size:0.75rem">▾</span>
              </div>
              @if (showRoleSwitcher()) {
                <div class="role-dropdown">
                  @for (role of availableRoles(); track role) {
                    <div
                      class="role-dropdown__item"
                      [class.active]="role === activeRole()"
                      (click)="switchRole(role); $event.stopPropagation()"
                    >
                      <span class="role-dropdown__icon">{{ roleIcon(role) }}</span>
                      <span>{{ role }}</span>
                    </div>
                  }
                </div>
              }
            </div>
          }

          <!-- Avatar with dropdown -->
          <div class="avatar-wrap" (click)="showAvatarMenu.set(!showAvatarMenu())">
            <div class="app-header__avatar" [title]="fullName()">{{ initials() }}</div>

            @if (showAvatarMenu()) {
              <div class="avatar-dropdown">
                <div class="avatar-dropdown__header">
                  <div class="avatar-dropdown__name">{{ fullName() }}</div>
                  <div class="avatar-dropdown__email">{{ userEmail() }}</div>
                </div>
                <div class="avatar-dropdown__divider"></div>
                <a class="avatar-dropdown__item" routerLink="/profile" (click)="showAvatarMenu.set(false)">
                  <span>👤</span> My Profile
                </a>
                <a class="avatar-dropdown__item" routerLink="/auth/change-password" (click)="showAvatarMenu.set(false)">
                  <span>🔑</span> Change Password
                </a>
                <div class="avatar-dropdown__divider"></div>
                <button class="avatar-dropdown__item avatar-dropdown__item--danger" (click)="logout()">
                  <span>🚪</span> Sign Out
                </button>
              </div>
            }
          </div>
        </div>
      </header>

      <!-- ── Sidebar ── -->
      <aside class="app-sidebar" [class.collapsed]="sidebarCollapsed()">
        <nav class="app-sidebar__nav">
          @for (section of visibleSections(); track section.title) {
            <div class="nav-section">
              <div class="nav-section-title">{{ section.title }}</div>
              @for (item of section.items; track item.route) {
                <a
                  class="nav-item"
                  [routerLink]="item.route"
                  routerLinkActive="active"
                  [title]="sidebarCollapsed() ? item.label : ''"
                >
                  <span class="nav-item__icon">{{ item.icon }}</span>
                  <span class="nav-label">{{ item.label }}</span>
                  @if (item.badge) {
                    <span class="nav-item__badge">{{ item.badge }}</span>
                  }
                </a>
              }
            </div>
          }
        </nav>

        <div class="app-sidebar__footer">
          <a class="nav-item" routerLink="/profile">
            <span class="nav-item__icon">👤</span>
            <span class="nav-label">My Profile</span>
          </a>
          <button class="nav-item" (click)="logout()">
            <span class="nav-item__icon">🚪</span>
            <span class="nav-label">Sign Out</span>
          </button>
        </div>
      </aside>

      <!-- ── Main Content ── -->
      <main class="app-main">
        <div class="page-content">
          <router-outlet />
        </div>
      </main>
    </div>
  `,
  styles: [`
    .role-switcher-wrap {
      position: relative;
      cursor: pointer;
    }

    .role-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: var(--surface-elevated);
      border-radius: var(--border-radius-md);
      box-shadow: var(--shadow-xl);
      border: 1px solid var(--color-neutral-200);
      min-width: 180px;
      z-index: 200;
      overflow: hidden;
      animation: fadeIn 150ms ease-out;

      &__item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 16px;
        font-size: 0.875rem;
        color: var(--color-neutral-700);
        cursor: pointer;
        transition: background 150ms;
        &:hover { background: var(--color-neutral-100); }
        &.active { background: var(--color-accent-100); color: var(--color-accent-700); font-weight: 600; }
      }

      &__icon { font-size: 1rem; }
    }

    .avatar-wrap {
      position: relative;
      cursor: pointer;
    }

    .avatar-dropdown {
      position: absolute;
      top: calc(100% + 10px);
      right: 0;
      background: var(--surface-elevated);
      border-radius: var(--border-radius-lg);
      box-shadow: var(--shadow-xl);
      border: 1px solid var(--color-neutral-200);
      min-width: 220px;
      z-index: 200;
      overflow: hidden;
      animation: fadeIn 150ms ease-out;

      &__header {
        padding: 14px 16px;
        background: var(--color-primary-50);
      }

      &__name {
        font-weight: 700;
        font-size: 0.9375rem;
        color: var(--color-neutral-900);
      }

      &__email {
        font-size: 0.8125rem;
        color: var(--color-neutral-500);
        margin-top: 2px;
      }

      &__divider {
        height: 1px;
        background: var(--color-neutral-200);
      }

      &__item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 11px 16px;
        font-size: 0.875rem;
        color: var(--color-neutral-700);
        cursor: pointer;
        transition: background 150ms;
        text-decoration: none;
        width: 100%;
        border: none;
        background: none;
        text-align: left;
        font-family: var(--font-body);

        &:hover { background: var(--color-neutral-100); }

        &--danger {
          color: var(--color-danger);
          &:hover { background: var(--color-danger-light); }
        }
      }
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to   { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class ShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  sidebarCollapsed = signal(this.loadSidebarState());
  showRoleSwitcher = signal(false);
  showAvatarMenu = signal(false);

  readonly activeRole = this.authService.activeRole;
  readonly fullName = this.authService.fullName;
  readonly initials = this.authService.initials;
  readonly availableRoles = computed(() => this.authService.user()?.roles ?? []);
  readonly userEmail = computed(() => this.authService.user()?.email ?? '');

  private readonly NAV_SECTIONS: NavSection[] = [
    {
      title: 'My Work',
      roles: ['EMPLOYEE', 'MANAGER', 'HR', 'ADMIN'],
      items: [
        { label: 'My Dashboard', icon: '📊', route: '/employee/dashboard' },
        { label: 'Log Time',     icon: '⏱',  route: '/employee/log-time' },
        { label: 'Weekly View',  icon: '📅',  route: '/employee/weekly-view' },
        { label: 'My History',   icon: '📋',  route: '/employee/history' },
      ]
    },
    {
      title: 'Team',
      roles: ['MANAGER'],
      items: [
        { label: 'Manager Dashboard', icon: '👥', route: '/manager/dashboard' },
        { label: 'Team Review',       icon: '✅', route: '/manager/team-review', badge: 5 },
        { label: 'Team Reports',      icon: '📈', route: '/manager/team-reports' },
      ]
    },
    {
      title: 'HR',
      roles: ['HR'],
      items: [
        { label: 'HR Dashboard',    icon: '🏢', route: '/hr/dashboard' },
        { label: 'Holiday Calendar',icon: '🏖️', route: '/hr/holiday-calendar' },
        { label: 'Reminders',       icon: '🔔', route: '/hr/reminders' },
        { label: 'HR Reports',      icon: '📊', route: '/hr/reports' },
      ]
    },
    {
      title: 'Administration',
      roles: ['ADMIN'],
      items: [
        { label: 'Users',                icon: '👤', route: '/admin/users' },
        { label: 'Projects',             icon: '🗂️', route: '/admin/projects' },
        { label: 'Manager Assignments',  icon: '🔗', route: '/admin/manager-assignments' },
        { label: 'Org Chart',            icon: '🌳', route: '/admin/org-chart' },
        { label: 'System Config',        icon: '⚙️', route: '/admin/system-config' },
        { label: 'Audit Log',            icon: '🔍', route: '/admin/audit-log' },
      ]
    }
  ];

  readonly visibleSections = computed(() => {
    const role = this.activeRole();
    if (!role) return [];
    return this.NAV_SECTIONS.filter(s => s.roles.includes(role));
  });

  ngOnInit(): void {
    // Collapse sidebar on small screens
    if (window.innerWidth < 1024) {
      this.sidebarCollapsed.set(true);
    }
  }

  toggleSidebar(): void {
    const next = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(next);
    localStorage.setItem('tms_sidebar_collapsed', String(next));
  }

  switchRole(role: Role): void {
    this.authService.switchRole(role);
    this.showRoleSwitcher.set(false);
    this.router.navigate([this.authService.getRoleDashboardRoute(role)]);
  }

  roleIcon(role: Role): string {
    const icons: Record<Role, string> = {
      EMPLOYEE: '👤', MANAGER: '👥', HR: '🏢', ADMIN: '⚙️'
    };
    return icons[role];
  }

  logout(): void {
    this.authService.logout();
  }

  private loadSidebarState(): boolean {
    return localStorage.getItem('tms_sidebar_collapsed') === 'true';
  }
}
