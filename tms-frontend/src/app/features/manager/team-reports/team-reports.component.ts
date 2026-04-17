import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-team-reports',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1 class="page-header__title">📈 Team Reports</h1>
    </div>

    <div class="card mb-4">
      <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:16px">Filters</h3>
      <div class="filters-row">
        <div class="form-group"><label class="form-label">Employee</label>
          <select class="form-control" [(ngModel)]="filters.employeeId">
            <option value="">All Team Members</option>
            @for (m of team(); track m.employeeId) {
              <option [value]="m.employeeId">{{ m.name }}</option>
            }
          </select>
        </div>
        <div class="form-group"><label class="form-label">From</label><input type="date" class="form-control" [(ngModel)]="filters.from" /></div>
        <div class="form-group"><label class="form-label">To</label><input type="date" class="form-control" [(ngModel)]="filters.to" /></div>
        <div class="form-group"><label class="form-label">Status</label>
          <select class="form-control" [(ngModel)]="filters.status">
            <option value="">All</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>
      <div class="flex gap-3 mt-4">
        <button class="btn btn--primary" (click)="load()">🔍 Search</button>
        <button class="btn btn--secondary" (click)="exportCsv()">⬇ Export CSV</button>
      </div>
    </div>

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else if (entries().length === 0) {
      <div class="card"><div class="empty-state"><div class="empty-state__icon">📊</div><div class="empty-state__title">No data for selected filters</div></div></div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead><tr><th>Employee</th><th>Date</th><th>Project</th><th>Task</th><th>Hours</th><th>Status</th></tr></thead>
          <tbody>
            @for (e of entries(); track e.id) {
              <tr>
                <td>{{ e.user?.fullName ?? '—' }}</td>
                <td>{{ formatDate(e.date) }}</td>
                <td>{{ e.project?.name ?? '—' }}</td>
                <td>{{ e.taskName }}</td>
                <td><strong>{{ e.hours }}h</strong></td>
                <td><span class="badge badge--{{ badgeClass(e.status) }}">{{ e.status.replace('_',' ') }}</span></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`.filters-row{display:flex;gap:16px;flex-wrap:wrap;}`]
})
export class TeamReportsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(false);
  team = signal<any[]>([]);
  entries = signal<any[]>([]);
  filters = { employeeId: '', from: this.monthAgo(), to: '', status: '' };

  ngOnInit() { this.loadTeam(); }

  async loadTeam() {
    try { this.team.set(await firstValueFrom(this.http.get<any[]>('/api/manager/team', {withCredentials:true})) ?? []); } catch {}
  }

  async load() {
    this.loading.set(true);
    try {
      // Use the weekly view endpoint per employee or history
      let url = `/api/timesheets/history?size=100`;
      if (this.filters.from) url += `&from=${this.filters.from}`;
      if (this.filters.to) url += `&to=${this.filters.to}`;
      if (this.filters.status) url += `&status=${this.filters.status}`;
      const res = await firstValueFrom(this.http.get<any>(url, {withCredentials:true}));
      this.entries.set(res.content ?? []);
    } catch {} finally { this.loading.set(false); }
  }

  exportCsv() { window.open(`/api/timesheets/history?export=csv&from=${this.filters.from}`, '_blank'); }
  formatDate(d: string) { return new Date(d+'T00:00:00').toLocaleDateString('en-US',{month:'short',day:'numeric',year:'numeric'}); }
  badgeClass(s: string) { const m: any={PENDING:'pending',APPROVED:'approved',REJECTED:'rejected',CLARIFICATION_REQUESTED:'clarification',AUTO_APPROVED:'auto-approved'}; return m[s]??'no-entries'; }
  private monthAgo() { const d=new Date(); d.setMonth(d.getMonth()-1); return d.toISOString().split('T')[0]; }
}
