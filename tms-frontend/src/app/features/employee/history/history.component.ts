import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-history',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">📋 My History</h1>
        <p class="page-header__subtitle">All your timesheet entries</p>
      </div>
      <div class="page-header__actions">
        <button class="btn btn--secondary btn--sm" (click)="exportCsv()">⬇ Export CSV</button>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-4" style="padding:16px 20px">
      <div class="filters-row">
        <div class="form-group">
          <label class="form-label">From</label>
          <input type="date" class="form-control" [(ngModel)]="filters.from" (change)="applyFilters()" />
        </div>
        <div class="form-group">
          <label class="form-label">To</label>
          <input type="date" class="form-control" [(ngModel)]="filters.to" (change)="applyFilters()" />
        </div>
        <div class="form-group" style="flex:1">
          <label class="form-label">Status</label>
          <select class="form-control" [(ngModel)]="filters.status" (change)="applyFilters()">
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="CLARIFICATION_REQUESTED">Clarification Requested</option>
            <option value="AUTO_APPROVED">Auto Approved</option>
          </select>
        </div>
        <button class="btn btn--ghost btn--sm" style="align-self:flex-end;height:44px" (click)="clearFilters()">
          ✕ Clear
        </button>
      </div>
    </div>

    <!-- Stats row -->
    @if (!loading() && entries().length > 0) {
      <div class="stats-row mb-4">
        <div class="stat-chip">
          <span class="stat-chip__value">{{ totalEntries() }}</span>
          <span class="stat-chip__label">Total Entries</span>
        </div>
        <div class="stat-chip stat-chip--amber">
          <span class="stat-chip__value">{{ pendingCount() }}</span>
          <span class="stat-chip__label">Pending</span>
        </div>
        <div class="stat-chip stat-chip--green">
          <span class="stat-chip__value">{{ approvedCount() }}</span>
          <span class="stat-chip__label">Approved</span>
        </div>
        <div class="stat-chip stat-chip--red">
          <span class="stat-chip__value">{{ rejectedCount() }}</span>
          <span class="stat-chip__label">Rejected</span>
        </div>
        <div class="stat-chip">
          <span class="stat-chip__value">{{ totalHours() }}h</span>
          <span class="stat-chip__label">Total Hours</span>
        </div>
      </div>
    }

    <!-- Table -->
    @if (loading()) {
      <div class="card" style="padding:32px">
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3,4,5]; track i) {
            <div class="skeleton skeleton--text" style="height:48px;border-radius:8px"></div>
          }
        </div>
      </div>
    } @else if (entries().length === 0) {
      <div class="card">
        <div class="empty-state">
          <div class="empty-state__icon">📋</div>
          <div class="empty-state__title">No entries found</div>
          <div class="empty-state__description">
            @if (hasFilters()) {
              Try adjusting your filters or
              <a (click)="clearFilters()" style="color:var(--color-primary-400);cursor:pointer">clear all filters</a>.
            } @else {
              Start logging time to see your history here.
            }
          </div>
          @if (!hasFilters()) {
            <a routerLink="/employee/log-time" class="btn btn--primary mt-4">⏱ Log Time Now</a>
          }
        </div>
      </div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Project</th>
              <th>Task</th>
              <th style="text-align:right">Hours</th>
              <th>Status</th>
              <th style="text-align:center">Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (e of entries(); track e.id) {
              <tr>
                <td style="white-space:nowrap">
                  <div style="font-weight:600;font-size:0.875rem">{{ formatDate(e.date) }}</div>
                  <div style="font-size:0.75rem;color:var(--color-neutral-400)">{{ formatDayName(e.date) }}</div>
                </td>
                <td>
                  @if (e.projectName) {
                    <div style="font-weight:500;font-size:0.875rem">{{ e.projectName }}</div>
                    <div style="font-size:0.75rem;color:var(--color-neutral-400)">{{ e.projectCode }}</div>
                  } @else {
                    <span style="color:var(--color-neutral-300)">—</span>
                  }
                </td>
                <td>
                  <div style="font-weight:500;font-size:0.9rem">{{ e.taskName }}</div>
                  @if (e.taskDescription) {
                    <div style="font-size:0.75rem;color:var(--color-neutral-500);margin-top:2px;max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
                      {{ e.taskDescription }}
                    </div>
                  }
                  @if (e.overtimeJustification) {
                    <div style="font-size:0.75rem;color:var(--color-warning);margin-top:2px">
                      ⚠️ {{ e.overtimeJustification }}
                    </div>
                  }
                </td>
                <td style="text-align:right">
                  <strong style="font-size:1rem">{{ e.hours }}h</strong>
                </td>
                <td>
                  <span class="badge badge--{{ badgeClass(e.status) }}">
                    {{ statusLabel(e.status) }}
                  </span>
                </td>
                <td style="text-align:center">
                  @if (e.status === 'PENDING' || e.status === 'CLARIFICATION_REQUESTED') {
                    <button
                      class="btn btn--danger btn--sm btn--icon"
                      title="Delete entry"
                      (click)="deleteEntry(e.id)">
                      🗑
                    </button>
                  } @else {
                    <span style="color:var(--color-neutral-300);font-size:0.75rem">locked</span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (totalPages() > 1) {
        <div class="pagination mt-4">
          <button class="btn btn--ghost btn--sm" [disabled]="page() === 0" (click)="changePage(-1)">← Prev</button>
          <span class="pagination__info">Page {{ page() + 1 }} of {{ totalPages() }}</span>
          <button class="btn btn--ghost btn--sm" [disabled]="page() >= totalPages() - 1" (click)="changePage(1)">Next →</button>
        </div>
      }
    }
  `,
  styles: [`
    .filters-row {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      align-items: flex-end;
      .form-group { min-width: 140px; }
    }

    .stats-row {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .stat-chip {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 10px 20px;
      background: var(--surface-elevated);
      border-radius: var(--border-radius-md);
      box-shadow: var(--shadow-sm);
      border: 1px solid var(--color-neutral-200);
      min-width: 90px;

      &__value { font-family: var(--font-display); font-size: 1.25rem; font-weight: 700; color: var(--color-neutral-900); }
      &__label { font-size: 0.6875rem; color: var(--color-neutral-500); text-transform: uppercase; letter-spacing: 0.06em; margin-top: 2px; }

      &--amber { border-color: var(--color-accent-200); background: var(--color-accent-100); .stat-chip__value { color: var(--color-accent-700); } }
      &--green  { border-color: var(--color-success-light); background: var(--color-success-light); .stat-chip__value { color: var(--color-success); } }
      &--red    { border-color: var(--color-danger-light); background: var(--color-danger-light); .stat-chip__value { color: var(--color-danger); } }
    }

    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      &__info { font-size: 0.875rem; color: var(--color-neutral-500); }
    }
  `]
})
export class HistoryComponent implements OnInit {
  private readonly http = inject(HttpClient);

  loading = signal(true);
  entries = signal<any[]>([]);
  page = signal(0);
  totalPages = signal(1);
  totalEntries = signal(0);

  filters = { from: this.threeMonthsAgo(), to: '', status: '' };

  pendingCount  = () => this.entries().filter(e => e.status === 'PENDING').length;
  approvedCount = () => this.entries().filter(e => e.status === 'APPROVED' || e.status === 'AUTO_APPROVED').length;
  rejectedCount = () => this.entries().filter(e => e.status === 'REJECTED').length;
  totalHours    = () => this.entries().reduce((s, e) => s + (e.hours ?? 0), 0).toFixed(1);
  hasFilters    = () => !!this.filters.status || !!this.filters.to;

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      let url = `/api/timesheets/history?page=${this.page()}&size=20&sort=date,desc`;
      if (this.filters.from) url += `&from=${this.filters.from}`;
      if (this.filters.to)   url += `&to=${this.filters.to}`;
      if (this.filters.status) url += `&status=${this.filters.status}`;
      const res = await firstValueFrom(this.http.get<any>(url, { withCredentials: true }));
      this.entries.set(res.content ?? []);
      this.totalPages.set(res.totalPages ?? 1);
      this.totalEntries.set(res.totalElements ?? 0);
    } catch { this.entries.set([]); }
    finally { this.loading.set(false); }
  }

  applyFilters() { this.page.set(0); this.load(); }
  clearFilters() { this.filters = { from: this.threeMonthsAgo(), to: '', status: '' }; this.applyFilters(); }
  changePage(d: number) { this.page.update(p => p + d); this.load(); }

  async deleteEntry(id: number) {
    if (!confirm('Delete this entry? This cannot be undone.')) return;
    try {
      await firstValueFrom(this.http.delete(`/api/timesheets/entries/${id}`, { withCredentials: true }));
      this.load();
    } catch {}
  }

  exportCsv() {
    const params = new URLSearchParams();
    if (this.filters.from) params.set('from', this.filters.from);
    if (this.filters.to)   params.set('to', this.filters.to);
    if (this.filters.status) params.set('status', this.filters.status);
    params.set('size', '1000');
    window.open(`/api/timesheets/history?${params.toString()}`, '_blank');
  }

  formatDate(d: string) {
    // Use substring(0,10) to get YYYY-MM-DD, then parse as local time
    const [y, m, day] = d.substring(0, 10).split('-').map(Number);
    return new Date(y, m - 1, day).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }
  formatDayName(d: string) {
    const [y, m, day] = d.substring(0, 10).split('-').map(Number);
    return new Date(y, m - 1, day).toLocaleDateString('en-US', { weekday: 'long' });
  }
  statusLabel(s: string) {
    const labels: Record<string, string> = {
      PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Rejected',
      CLARIFICATION_REQUESTED: 'Clarification', AUTO_APPROVED: 'Auto Approved'
    };
    return labels[s] ?? s;
  }
  badgeClass(s: string) {
    const m: Record<string, string> = {
      PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected',
      CLARIFICATION_REQUESTED: 'clarification', AUTO_APPROVED: 'auto-approved'
    };
    return m[s] ?? 'no-entries';
  }
  private threeMonthsAgo() {
    const d = new Date(); d.setMonth(d.getMonth() - 3); return d.toISOString().split('T')[0];
  }
}
