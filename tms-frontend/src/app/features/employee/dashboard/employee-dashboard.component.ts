import {
  Component, inject, signal, computed, OnInit, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { EmployeeDashboard } from '../../../core/models/timesheet.model';

@Component({
  selector: 'tms-employee-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dot-grid-bg" style="margin: -28px -32px; padding: 28px 32px; min-height: 100%;">

      <!-- Page Header -->
      <div class="page-header stagger-children">
        <div>
          <h1 class="page-header__title">Good {{ greeting() }}, {{ firstName() }} 👋</h1>
          <p class="page-header__subtitle">{{ today() }} · {{ timezone() }}</p>
        </div>
        <div class="page-header__actions">
          <a routerLink="/employee/log-time" class="btn btn--primary">
            ⏱ Log Time
          </a>
        </div>
      </div>

      <!-- KPI Cards -->
      @if (loading()) {
        <div class="grid-4 stagger-children">
          @for (i of [1,2,3,4]; track i) {
            <div class="skeleton skeleton--card"></div>
          }
        </div>
      } @else {
        <div class="grid-4 stagger-children">
          <div class="kpi-card">
            <div class="kpi-card__icon kpi-card__icon--blue">⏱</div>
            <div class="kpi-card__label">Hours This Week</div>
            <div class="kpi-card__value">{{ dashboard()?.totalHoursThisWeek ?? 0 }}</div>
            <div class="kpi-card__trend">of 40 hrs target</div>
          </div>

          <div class="kpi-card">
            <div class="kpi-card__icon kpi-card__icon--amber">⏳</div>
            <div class="kpi-card__label">Pending Approval</div>
            <div class="kpi-card__value">{{ dashboard()?.pendingCount ?? 0 }}</div>
            <div class="kpi-card__trend">entries awaiting review</div>
          </div>

          <div class="kpi-card">
            <div class="kpi-card__icon kpi-card__icon--green">✅</div>
            <div class="kpi-card__label">Approved</div>
            <div class="kpi-card__value">{{ dashboard()?.approvedCount ?? 0 }}</div>
            <div class="kpi-card__trend">this month</div>
          </div>

          <div class="kpi-card" [class.card--active]="(dashboard()?.missedDates?.length ?? 0) > 0">
            <div class="kpi-card__icon kpi-card__icon--red">⚠️</div>
            <div class="kpi-card__label">Missed Dates</div>
            <div class="kpi-card__value">{{ dashboard()?.missedDates?.length ?? 0 }}</div>
            <div class="kpi-card__trend">
              @if ((dashboard()?.missedDates?.length ?? 0) > 0) {
                <span style="color: var(--color-danger)">Action needed</span>
              } @else {
                All caught up!
              }
            </div>
          </div>
        </div>
      }

      <!-- Bottom grid -->
      <div class="dashboard-grid mt-6">

        <!-- Missed Dates -->
        <div class="card">
          <div class="card-section-header">
            <h3>Missed Dates</h3>
            @if ((dashboard()?.missedDates?.length ?? 0) > 0) {
              <span class="badge badge--pending">{{ dashboard()?.missedDates?.length }} pending</span>
            }
          </div>

          @if (loading()) {
            <div class="flex flex-col gap-2 mt-4">
              @for (i of [1,2,3]; track i) {
                <div class="skeleton skeleton--text"></div>
              }
            </div>
          } @else if ((dashboard()?.missedDates?.length ?? 0) === 0) {
            <div class="empty-state" style="padding: 32px 16px;">
              <div class="empty-state__icon">🎉</div>
              <div class="empty-state__title">All caught up!</div>
              <div class="empty-state__description">No missed dates this week.</div>
            </div>
          } @else {
            <div class="missed-dates-list mt-4">
              @for (date of dashboard()?.missedDates; track date) {
                <div class="missed-date-item">
                  <div class="missed-date-item__info">
                    <span class="missed-date-item__icon">📅</span>
                    <div>
                      <div class="missed-date-item__date">{{ formatDate(date) }}</div>
                      <div class="missed-date-item__label text-muted text-sm">No entries logged</div>
                    </div>
                  </div>
                  <a [routerLink]="['/employee/log-time']" [queryParams]="{date}" class="btn btn--sm btn--primary">
                    Log Now
                  </a>
                </div>
              }
            </div>
          }
        </div>

        <!-- Recent Activity -->
        <div class="card">
          <div class="card-section-header">
            <h3>Recent Activity</h3>
            <a routerLink="/employee/history" class="text-sm" style="color: var(--color-primary-400)">
              View all →
            </a>
          </div>

          @if (loading()) {
            <div class="flex flex-col gap-3 mt-4">
              @for (i of [1,2,3,4,5]; track i) {
                <div class="flex gap-3 items-center">
                  <div class="skeleton skeleton--avatar"></div>
                  <div class="flex flex-col gap-1" style="flex:1">
                    <div class="skeleton skeleton--text" style="width:70%"></div>
                    <div class="skeleton skeleton--text" style="width:40%"></div>
                  </div>
                </div>
              }
            </div>
          } @else if ((dashboard()?.recentActivity?.length ?? 0) === 0) {
            <div class="empty-state" style="padding: 32px 16px;">
              <div class="empty-state__icon">📋</div>
              <div class="empty-state__title">No activity yet</div>
              <div class="empty-state__description">Start logging time to see your activity here.</div>
            </div>
          } @else {
            <div class="activity-feed mt-4">
              @for (item of dashboard()?.recentActivity; track item.id) {
                <div class="activity-item">
                  <div class="activity-item__dot" [class]="'activity-item__dot--' + activityColor(item.type)"></div>
                  <div class="activity-item__content">
                    <div class="activity-item__message">{{ item.message }}</div>
                    <div class="activity-item__time text-muted text-xs">{{ formatRelative(item.timestamp) }}</div>
                  </div>
                </div>
              }
            </div>
          }
        </div>

      </div>
    </div>
  `,
  styles: [`
    .dashboard-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;

      @media (max-width: 768px) { grid-template-columns: 1fr; }
    }

    .card-section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 4px;

      h3 {
        font-family: var(--font-display);
        font-size: 1rem;
        font-weight: 600;
        color: var(--color-neutral-800);
      }
    }

    .missed-dates-list { display: flex; flex-direction: column; gap: 10px; }

    .missed-date-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 14px;
      background: var(--color-warning-light);
      border-radius: var(--border-radius-md);
      border-left: 3px solid var(--color-warning);

      &__info { display: flex; align-items: center; gap: 10px; }
      &__icon { font-size: 1.25rem; }
      &__date { font-weight: 600; font-size: 0.9rem; color: var(--color-neutral-800); }
    }

    .activity-feed { display: flex; flex-direction: column; gap: 0; }

    .activity-item {
      display: flex;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid var(--color-neutral-100);

      &:last-child { border-bottom: none; }

      &__dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-top: 6px;
        flex-shrink: 0;

        &--green  { background: var(--color-success); }
        &--amber  { background: var(--color-accent-500); }
        &--red    { background: var(--color-danger); }
        &--blue   { background: var(--color-info); }
        &--gray   { background: var(--color-neutral-400); }
      }

      &__message { font-size: 0.875rem; color: var(--color-neutral-700); line-height: 1.4; }
      &__time    { margin-top: 2px; }
    }
  `]
})
export class EmployeeDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  loading = signal(true);
  dashboard = signal<EmployeeDashboard | null>(null);

  readonly firstName = computed(() => {
    const name = this.authService.fullName();
    return name.split(' ')[0];
  });

  readonly timezone = computed(() => this.authService.user()?.timezone ?? 'UTC');

  greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'morning';
    if (h < 17) return 'afternoon';
    return 'evening';
  }

  today(): string {
    return new Date().toLocaleDateString('en-US', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });
  }

  ngOnInit(): void {
    this.loadDashboard();
  }

  private async loadDashboard(): Promise<void> {
    try {
      const data = await firstValueFrom(
        this.http.get<EmployeeDashboard>('/api/timesheets/dashboard', { withCredentials: true })
      );
      this.dashboard.set(data ?? null);
    } catch {
      // Show empty state on error
      this.dashboard.set({
        totalHoursThisWeek: 0,
        pendingCount: 0,
        approvedCount: 0,
        missedDates: [],
        recentActivity: []
      });
    } finally {
      this.loading.set(false);
    }
  }

  formatDate(isoDate: string): string {
    return new Date(isoDate).toLocaleDateString('en-US', {
      weekday: 'short', month: 'short', day: 'numeric'
    });
  }

  formatRelative(isoDate: string): string {
    const diff = Date.now() - new Date(isoDate).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1)  return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24)  return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  }

  activityColor(type: string): string {
    if (type.includes('APPROVED')) return 'green';
    if (type.includes('REJECTED')) return 'red';
    if (type.includes('CLARIFICATION')) return 'blue';
    if (type.includes('SUBMITTED')) return 'amber';
    return 'gray';
  }
}
