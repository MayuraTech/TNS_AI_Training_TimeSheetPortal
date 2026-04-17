import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-manager-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dot-grid-bg" style="margin:-28px -32px;padding:28px 32px;min-height:100%">
      <div class="page-header">
        <h1 class="page-header__title">👥 Manager Dashboard</h1>
        <div class="page-header__actions">
          <a routerLink="/manager/team-review" class="btn btn--primary">Review Team →</a>
        </div>
      </div>

      @if (loading()) {
        <div class="grid-4"><div class="skeleton skeleton--card" *ngFor="let i of [1,2,3,4]"></div></div>
      } @else {
        <div class="grid-4 stagger-children">
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--blue">👥</div><div class="kpi-card__label">Direct Reports</div><div class="kpi-card__value">{{ kpis()?.totalDirectReports ?? 0 }}</div></div>
          <div class="kpi-card card--active"><div class="kpi-card__icon kpi-card__icon--amber">⏳</div><div class="kpi-card__label">Pending Approvals</div><div class="kpi-card__value">{{ kpis()?.pendingApprovals ?? 0 }}</div></div>
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--green">✅</div><div class="kpi-card__label">Approved This Week</div><div class="kpi-card__value">{{ kpis()?.approvedThisWeek ?? 0 }}</div></div>
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--red">⚠️</div><div class="kpi-card__label">Missed Entries</div><div class="kpi-card__value">{{ missedCount() }}</div></div>
        </div>
      }

      <div class="card mt-6">
        <div class="card-section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
          <h3 style="font-family:var(--font-display);font-size:1rem;font-weight:600">Team Summary</h3>
          <a routerLink="/manager/team-review" style="font-size:0.875rem;color:var(--color-primary-400)">View all →</a>
        </div>
        @if (loading()) {
          <div class="skeleton skeleton--card"></div>
        } @else if (team().length === 0) {
          <div class="empty-state"><div class="empty-state__icon">👥</div><div class="empty-state__title">No direct reports</div></div>
        } @else {
          <table class="tms-table">
            <thead><tr><th>Name</th><th>Hours This Week</th><th>Pending</th><th>Action</th></tr></thead>
            <tbody>
              @for (m of team(); track m.employeeId) {
                <tr>
                  <td><strong>{{ m.name }}</strong></td>
                  <td>{{ m.hoursThisWeek | number:'1.1-1' }}h</td>
                  <td>
                    @if (m.pendingCount > 0) {
                      <span class="badge badge--pending">{{ m.pendingCount }} pending</span>
                    } @else { <span class="badge badge--approved">Up to date</span> }
                  </td>
                  <td><a [routerLink]="['/manager/team-review']" [queryParams]="{employeeId: m.employeeId}" class="btn btn--sm btn--secondary">Review</a></td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </div>
  `
})
export class ManagerDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(true);
  kpis = signal<any>(null);
  team = signal<any[]>([]);
  missedCount = signal(0);

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      const [kpisRes, teamRes] = await Promise.all([
        firstValueFrom(this.http.get<any>('/api/manager/dashboard', {withCredentials:true})),
        firstValueFrom(this.http.get<any[]>('/api/manager/team', {withCredentials:true}))
      ]);
      this.kpis.set(kpisRes);
      const sorted = (teamRes ?? []).sort((a: any, b: any) => b.pendingCount - a.pendingCount);
      this.team.set(sorted);
    } catch { } finally { this.loading.set(false); }
  }
}
