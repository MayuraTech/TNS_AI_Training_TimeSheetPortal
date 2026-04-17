import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-audit-log',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1 class="page-header__title">🔍 Audit Log</h1>
      <div class="page-header__actions">
        <span style="font-size:0.8125rem;color:var(--color-neutral-500)">Read-only · Immutable</span>
      </div>
    </div>

    <div class="card mb-4">
      <div class="filters-row">
        <div class="form-group"><label class="form-label">Action Type</label>
          <select class="form-control" [(ngModel)]="filters.actionType" (change)="load()">
            <option value="">All Actions</option>
            <option value="LOGIN">Login</option>
            <option value="TIMESHEET_SUBMITTED">Timesheet Submitted</option>
            <option value="ENTRY_APPROVED">Entry Approved</option>
            <option value="ENTRY_REJECTED">Entry Rejected</option>
            <option value="USER_CREATED">User Created</option>
            <option value="PROJECT_CREATED">Project Created</option>
            <option value="CONFIG_UPDATED">Config Updated</option>
            <option value="PASSWORD_CHANGED">Password Changed</option>
          </select>
        </div>
        <div class="form-group"><label class="form-label">Entity Type</label>
          <select class="form-control" [(ngModel)]="filters.entityType" (change)="load()">
            <option value="">All Entities</option>
            <option value="USER">User</option>
            <option value="TIMESHEET_ENTRY">Timesheet Entry</option>
            <option value="PROJECT">Project</option>
            <option value="SYSTEM_CONFIG">System Config</option>
          </select>
        </div>
        <button class="btn btn--ghost btn--sm" style="align-self:flex-end" (click)="clearFilters()">Clear</button>
      </div>
    </div>

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else if (logs().length === 0) {
      <div class="card"><div class="empty-state"><div class="empty-state__icon">🔍</div><div class="empty-state__title">No audit logs found</div></div></div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead><tr><th>Timestamp</th><th>Actor</th><th>Action</th><th>Entity</th><th>Before</th><th>After</th></tr></thead>
          <tbody>
            @for (log of logs(); track log.id) {
              <tr>
                <td style="white-space:nowrap;font-size:0.8125rem">{{ formatDate(log.createdAt) }}</td>
                <td>User #{{ log.actorId }}</td>
                <td><span class="action-badge" [class]="actionClass(log.actionType)">{{ log.actionType.replace('_',' ') }}</span></td>
                <td>{{ log.entityType }}@if(log.entityId){ #{{ log.entityId }}}</td>
                <td style="font-size:0.8125rem;color:var(--color-neutral-500);max-width:150px;overflow:hidden;text-overflow:ellipsis">{{ log.beforeValue ?? '—' }}</td>
                <td style="font-size:0.8125rem;max-width:150px;overflow:hidden;text-overflow:ellipsis">{{ log.afterValue ?? '—' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div class="pagination mt-4">
        <button class="btn btn--ghost btn--sm" [disabled]="page()===0" (click)="changePage(-1)">← Prev</button>
        <span style="font-size:0.875rem;color:var(--color-neutral-500)">Page {{ page()+1 }} of {{ totalPages() }}</span>
        <button class="btn btn--ghost btn--sm" [disabled]="page()>=totalPages()-1" (click)="changePage(1)">Next →</button>
      </div>
    }
  `,
  styles: [`.filters-row{display:flex;gap:16px;flex-wrap:wrap;}.action-badge{padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;background:var(--color-neutral-100);color:var(--color-neutral-700);}.pagination{display:flex;align-items:center;justify-content:center;gap:16px;}`]
})
export class AuditLogComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(true);
  logs = signal<any[]>([]);
  page = signal(0);
  totalPages = signal(1);
  filters = { actionType: '', entityType: '' };

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      let url = `/api/admin/audit-log?page=${this.page()}&size=20&sort=createdAt,desc`;
      if (this.filters.actionType) url += `&actionType=${this.filters.actionType}`;
      if (this.filters.entityType) url += `&entityType=${this.filters.entityType}`;
      const res = await firstValueFrom(this.http.get<any>(url, {withCredentials:true}));
      this.logs.set(res.content ?? []);
      this.totalPages.set(res.totalPages ?? 1);
    } catch {} finally { this.loading.set(false); }
  }

  clearFilters() { this.filters = {actionType:'',entityType:''}; this.page.set(0); this.load(); }
  changePage(d: number) { this.page.update(p => p+d); this.load(); }
  formatDate(d: string) { return new Date(d).toLocaleString('en-IN',{day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit'}); }
  actionClass(a: string) { if(a.includes('APPROVED'))return'action-badge--green'; if(a.includes('REJECTED'))return'action-badge--red'; if(a.includes('LOGIN'))return'action-badge--blue'; return ''; }
}
