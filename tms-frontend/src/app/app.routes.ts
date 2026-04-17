import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Auth routes — no shell
  {
    path: 'auth',
    loadComponent: () => import('./features/auth/auth-layout/auth-layout.component')
      .then(m => m.AuthLayoutComponent),
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component')
          .then(m => m.LoginComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password/forgot-password.component')
          .then(m => m.ForgotPasswordComponent)
      },
      {
        path: 'reset-password',
        loadComponent: () => import('./features/auth/reset-password/reset-password.component')
          .then(m => m.ResetPasswordComponent)
      },
      {
        path: 'change-password',
        canActivate: [authGuard],
        loadComponent: () => import('./features/auth/change-password/change-password.component')
          .then(m => m.ChangePasswordComponent)
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // App shell routes
  {
    path: '',
    loadComponent: () => import('./shared/components/shell/shell.component')
      .then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      // Employee
      {
        path: 'employee',
        children: [
          {
            path: 'dashboard',
            loadComponent: () => import('./features/employee/dashboard/employee-dashboard.component')
              .then(m => m.EmployeeDashboardComponent)
          },
          {
            path: 'log-time',
            loadComponent: () => import('./features/employee/log-time/log-time.component')
              .then(m => m.LogTimeComponent)
          },
          {
            path: 'weekly-view',
            loadComponent: () => import('./features/employee/weekly-view/weekly-view.component')
              .then(m => m.WeeklyViewComponent)
          },
          {
            path: 'history',
            loadComponent: () => import('./features/employee/history/history.component')
              .then(m => m.HistoryComponent)
          },
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
      },

      // Manager
      {
        path: 'manager',
        canActivate: [roleGuard('MANAGER')],
        children: [
          {
            path: 'dashboard',
            loadComponent: () => import('./features/manager/dashboard/manager-dashboard.component')
              .then(m => m.ManagerDashboardComponent)
          },
          {
            path: 'team-review',
            loadComponent: () => import('./features/manager/team-review/team-review.component')
              .then(m => m.TeamReviewComponent)
          },
          {
            path: 'team-reports',
            loadComponent: () => import('./features/manager/team-reports/team-reports.component')
              .then(m => m.TeamReportsComponent)
          },
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
      },

      // HR
      {
        path: 'hr',
        canActivate: [roleGuard('HR')],
        children: [
          {
            path: 'dashboard',
            loadComponent: () => import('./features/hr/dashboard/hr-dashboard.component')
              .then(m => m.HrDashboardComponent)
          },
          {
            path: 'holiday-calendar',
            loadComponent: () => import('./features/hr/holiday-calendar/holiday-calendar.component')
              .then(m => m.HolidayCalendarComponent)
          },
          {
            path: 'reminders',
            loadComponent: () => import('./features/hr/reminders/reminders.component')
              .then(m => m.RemindersComponent)
          },
          {
            path: 'reports',
            loadComponent: () => import('./features/hr/reports/hr-reports.component')
              .then(m => m.HrReportsComponent)
          },
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
      },

      // Admin
      {
        path: 'admin',
        canActivate: [roleGuard('ADMIN')],
        children: [
          {
            path: 'users',
            loadComponent: () => import('./features/admin/users/users.component')
              .then(m => m.UsersComponent)
          },
          {
            path: 'projects',
            loadComponent: () => import('./features/admin/projects/projects.component')
              .then(m => m.ProjectsComponent)
          },
          {
            path: 'org-chart',
            loadComponent: () => import('./features/admin/org-chart/org-chart.component')
              .then(m => m.OrgChartComponent)
          },
          {
            path: 'manager-assignments',
            loadComponent: () => import('./features/admin/manager-assignments/manager-assignments.component')
              .then(m => m.ManagerAssignmentsComponent)
          },
          {
            path: 'system-config',
            loadComponent: () => import('./features/admin/system-config/system-config.component')
              .then(m => m.SystemConfigComponent)
          },
          {
            path: 'audit-log',
            loadComponent: () => import('./features/admin/audit-log/audit-log.component')
              .then(m => m.AuditLogComponent)
          },
          { path: '', redirectTo: 'users', pathMatch: 'full' }
        ]
      },

      { path: '', redirectTo: 'employee/dashboard', pathMatch: 'full' },

      // Profile — inside shell so sidebar shows
      {
        path: 'profile',
        loadComponent: () => import('./features/employee/profile/profile.component')
          .then(m => m.ProfileComponent)
      }
    ]
  },

  { path: '**', redirectTo: '/auth/login' }
];
