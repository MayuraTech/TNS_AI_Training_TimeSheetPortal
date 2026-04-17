import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-hr-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="dot-grid-bg" style="margin:-28px -32px;padding:28px 32px;min-height:100%">
      <div class="page-header"><h1 class="page-header__title">🏢 HR Dashboard</h1></div>

      @if (loading()) {
        <div class="grid-4"><div class="skeleton skeleton--card" style="height:120px" *ngFor="let i of [1,2,3,4]"></div></div>
      } @else {
        <div class="grid-4 stagger-children">
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--blue">👥</div><div class="kpi-card__label">Total Employees</div><div class="kpi-card__value">{{ data()?.totalEmployees ?? 0 }}</div></div>
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--green">📊</div><div class="kpi-card__label">Compliance Rate</div><div class="kpi-card__value">{{ data()?.complianceRate ?? 0 }}%</div><div class="kpi-card__trend">this week</div></div>
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--amber">⏱</div><div class="kpi-card__label">Avg Hours / Employee</div><div class="kpi-card__value">{{ data()?.avgHoursPerEmployee ?? 0 }}</div><div class="kpi-card__trend">this week</div></div>
          <div class="kpi-card"><div class="kpi-card__icon kpi-card__icon--red">⏳</div><div class="kpi-card__label">Pending Org-Wide</div><div class="kpi-card__value">—</div></div>
        </div>
      }

      <div class="card mt-6">
        <h3 style="font-family:var(--font-display);font-size:1rem;font-weight:600;margin-bottom:16px">Quick Actions</h3>
        <div class="flex gap-3 flex-wrap">
          <a href="/hr/reminders" class="btn btn--secondary">🔔 Send Reminders</a>
          <a href="/hr/reports" class="btn btn--secondary">📊 Generate Report</a>
          <a href="/hr/holiday-calendar" class="btn btn--secondary">🏖️ Manage Holidays</a>
        </div>
      </div>
    </div>
  `
})
export class HrDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(true);
  data = signal<any>(null);

  ngOnInit() { this.load(); }
  async load() {
    try { this.data.set(await firstValueFrom(this.http.get<any>('/api/hr/dashboard', {withCredentials:true}))); }
    catch {} finally { this.loading.set(false); }
  }
}
