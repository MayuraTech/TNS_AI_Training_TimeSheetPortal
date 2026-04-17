import { Component, inject, signal, computed, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-team-review',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1 class="page-header__title">✅ Team Review</h1>
    </div>

    <!-- Employee selector -->
    <div class="card mb-4">
      <div class="filters-row">
        <div class="form-group" style="flex:1">
          <label class="form-label">Employee</label>
          <select class="form-control" [(ngModel)]="selectedEmployeeId" (change)="loadEntries()">
            <option [ngValue]="null">Select employee...</option>
            @for (m of team(); track m.employeeId) {
              <option [ngValue]="m.employeeId">{{ m.name }} @if(m.pendingCount>0){({{ m.pendingCount }} pending)}</option>
            }
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Week</label>
          <input type="date" class="form-control" [(ngModel)]="weekStart" (change)="loadEntries()" />
        </div>
      </div>
    </div>

    @if (selectedEmployeeId && !loading()) {
      <!-- Bulk actions -->
      <div class="bulk-actions mb-4">
        <button class="btn btn--success btn--sm" (click)="bulkApproveAll()">✅ Approve All Pending</button>
        <button class="btn btn--danger btn--sm" (click)="showBulkReject=true">✕ Reject All Pending</button>
        @if (showBulkReject) {
          <div class="inline-reject">
            <input class="form-control" [(ngModel)]="bulkRejectReason" placeholder="Rejection reason (min 10 chars)" style="width:300px" />
            <button class="btn btn--danger btn--sm" (click)="bulkRejectAll()">Confirm Reject</button>
            <button class="btn btn--ghost btn--sm" (click)="showBulkReject=false">Cancel</button>
          </div>
        }
      </div>

      <!-- Entries grouped by day -->
      @for (day of groupedDays(); track day.date) {
        <div class="day-group card mb-4">
          <div class="day-group__header">
            <div>
              <span class="day-group__date">{{ formatDate(day.date) }}</span>
              <span class="badge badge--{{ badgeClass(day.dayStatus) }} ml-2">{{ day.dayStatus.replace('_',' ') }}</span>
              @if (day.hasOvertime) { <span class="overtime-badge">⚠️ Overtime</span> }
            </div>
            <div class="day-group__actions">
              <span class="day-group__total">{{ day.totalHours | number:'1.1-1' }}h total</span>
              <button class="btn btn--success btn--sm" (click)="approveDay(day.date)">Approve Day</button>
            </div>
          </div>

          @for (entry of day.entries; track entry.id) {
            <div class="entry-row" [class.entry-row--overtime]="entry.hours >= 9">
              <div class="entry-row__info">
                <div class="entry-row__task">{{ entry.taskName }}</div>
                <div class="entry-row__meta">{{ entry.project?.name }} · {{ entry.hours }}h</div>
                @if (entry.taskDescription) {
                  <div class="entry-row__desc">{{ entry.taskDescription }}</div>
                }
                @if (entry.overtimeJustification) {
                  <div class="entry-row__overtime-note">⚠️ {{ entry.overtimeJustification }}</div>
                }
              </div>
              <div class="entry-row__status">
                <span class="badge badge--{{ badgeClass(entry.status) }}">{{ entry.status.replace('_',' ') }}</span>
              </div>
              @if (entry.status === 'PENDING' || entry.status === 'CLARIFICATION_REQUESTED') {
                <div class="entry-row__actions">
                  <button class="btn btn--success btn--sm" (click)="approveEntry(entry.id)">✅</button>
                  <button class="btn btn--ghost btn--sm" (click)="requestClarification(entry.id)">💬</button>
                  <button class="btn btn--danger btn--sm" (click)="openReject(entry.id)">✕</button>
                </div>
              }
            </div>
          }
        </div>
      }

      @if (groupedDays().length === 0) {
        <div class="card"><div class="empty-state"><div class="empty-state__icon">📋</div><div class="empty-state__title">No entries this week</div></div></div>
      }
    }

    <!-- Reject modal -->
    @if (rejectEntryId) {
      <div class="modal-overlay" (click)="rejectEntryId=null">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <h3>Reject Entry</h3>
          <div class="form-group mt-4">
            <label class="form-label">Reason <span style="color:var(--color-danger)">*</span></label>
            <textarea class="form-control" [(ngModel)]="rejectReason" rows="3" placeholder="Min 10 characters..."></textarea>
          </div>
          <div class="flex gap-3 mt-4">
            <button class="btn btn--danger" (click)="confirmReject()">Reject</button>
            <button class="btn btn--ghost" (click)="rejectEntryId=null">Cancel</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .filters-row { display:flex; gap:16px; flex-wrap:wrap; }
    .bulk-actions { display:flex; gap:12px; align-items:center; flex-wrap:wrap; }
    .inline-reject { display:flex; gap:8px; align-items:center; }
    .day-group { &__header { display:flex; justify-content:space-between; align-items:center; padding-bottom:12px; border-bottom:1px solid var(--color-neutral-100); margin-bottom:12px; } &__date { font-weight:700; font-size:1rem; } &__actions { display:flex; gap:8px; align-items:center; } &__total { font-size:0.875rem; color:var(--color-neutral-500); } }
    .overtime-badge { background:var(--color-warning-light); color:var(--color-warning); padding:2px 8px; border-radius:4px; font-size:0.75rem; font-weight:600; margin-left:8px; }
    .entry-row { display:flex; align-items:flex-start; gap:16px; padding:12px 0; border-bottom:1px solid var(--color-neutral-100); &:last-child{border:none} &--overtime { border-left:3px solid var(--color-accent-500); padding-left:12px; } &__info { flex:1; } &__task { font-weight:600; font-size:0.9375rem; } &__meta { font-size:0.8125rem; color:var(--color-neutral-500); margin-top:2px; } &__desc { font-size:0.8125rem; color:var(--color-neutral-600); margin-top:4px; } &__overtime-note { font-size:0.8125rem; color:var(--color-warning); margin-top:4px; background:var(--color-warning-light); padding:4px 8px; border-radius:4px; } &__status { flex-shrink:0; } &__actions { display:flex; gap:6px; flex-shrink:0; } }
    .ml-2 { margin-left:8px; }
    .modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,0.5); display:flex; align-items:center; justify-content:center; z-index:1000; }
    .modal-card { background:var(--surface-elevated); border-radius:var(--border-radius-xl); padding:32px; width:100%; max-width:480px; box-shadow:var(--shadow-xl); h3{font-family:var(--font-display);font-size:1.25rem;font-weight:700;} }
  `]
})
export class TeamReviewComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);

  loading = signal(false);
  team = signal<any[]>([]);
  entries = signal<any[]>([]);
  selectedEmployeeId: number | null = null;
  weekStart = this.getMonday();
  rejectEntryId: number | null = null;
  rejectReason = '';
  showBulkReject = false;
  bulkRejectReason = '';

  readonly groupedDays = computed(() => {
    const map = new Map<string, any[]>();
    for (const e of this.entries()) {
      const d = (e.date ?? '').split('T')[0];
      if (!map.has(d)) map.set(d, []);
      map.get(d)!.push(e);
    }
    return Array.from(map.entries()).sort(([a],[b]) => a.localeCompare(b)).map(([date, entries]) => {
      const totalHours = entries.reduce((s: number, e: any) => s + e.hours, 0);
      const statuses = entries.map((e: any) => e.status);
      const hasOvertime = totalHours > 9;
      let dayStatus = 'PENDING';
      if (statuses.some((s: string) => s === 'REJECTED')) dayStatus = 'REJECTED';
      else if (statuses.some((s: string) => s === 'CLARIFICATION_REQUESTED')) dayStatus = 'CLARIFICATION_REQUESTED';
      else if (statuses.every((s: string) => s === 'APPROVED' || s === 'AUTO_APPROVED')) dayStatus = 'APPROVED';
      return { date, entries, totalHours, dayStatus, hasOvertime };
    });
  });

  ngOnInit() {
    this.loadTeam();
    this.route.queryParams.subscribe(p => {
      if (p['employeeId']) { this.selectedEmployeeId = +p['employeeId']; this.loadEntries(); }
    });
  }

  async loadTeam() {
    try { this.team.set(await firstValueFrom(this.http.get<any[]>('/api/manager/team', {withCredentials:true})) ?? []); } catch {}
  }

  async loadEntries() {
    if (!this.selectedEmployeeId) return;
    this.loading.set(true);
    try {
      const res = await firstValueFrom(this.http.get<any[]>(`/api/manager/team/${this.selectedEmployeeId}/week?weekStart=${this.weekStart}`, {withCredentials:true}));
      this.entries.set(res ?? []);
    } catch { this.entries.set([]); } finally { this.loading.set(false); }
  }

  async approveEntry(id: number) {
    try { await firstValueFrom(this.http.post(`/api/approvals/entries/${id}/approve`, {}, {withCredentials:true})); this.loadEntries(); } catch {}
  }

  async approveDay(date: string) {
    try { await firstValueFrom(this.http.post(`/api/approvals/day/${this.selectedEmployeeId}/${date}/approve`, {}, {withCredentials:true})); this.loadEntries(); } catch {}
  }

  async bulkApproveAll() {
    const pending = this.entries().filter(e => e.status === 'PENDING');
    for (const e of pending) await this.approveEntry(e.id);
  }

  async bulkRejectAll() {
    if (this.bulkRejectReason.trim().length < 10) return alert('Reason must be at least 10 characters');
    const pending = this.entries().filter(e => e.status === 'PENDING');
    for (const e of pending) {
      try { await firstValueFrom(this.http.post(`/api/approvals/entries/${e.id}/reject`, {reason: this.bulkRejectReason}, {withCredentials:true})); } catch {}
    }
    this.showBulkReject = false; this.bulkRejectReason = ''; this.loadEntries();
  }

  openReject(id: number) { this.rejectEntryId = id; this.rejectReason = ''; }

  async confirmReject() {
    if (!this.rejectEntryId || this.rejectReason.trim().length < 10) return;
    try { await firstValueFrom(this.http.post(`/api/approvals/entries/${this.rejectEntryId}/reject`, {reason: this.rejectReason}, {withCredentials:true})); this.rejectEntryId = null; this.loadEntries(); } catch {}
  }

  async requestClarification(id: number) {
    try { await firstValueFrom(this.http.post(`/api/approvals/entries/${id}/clarify`, {}, {withCredentials:true})); this.loadEntries(); } catch {}
  }

  formatDate(d: string) { return new Date(d+'T00:00:00').toLocaleDateString('en-US',{weekday:'long',month:'short',day:'numeric'}); }
  badgeClass(s: string) { const m: any={PENDING:'pending',APPROVED:'approved',REJECTED:'rejected',CLARIFICATION_REQUESTED:'clarification',AUTO_APPROVED:'auto-approved',NO_ENTRIES:'no-entries'}; return m[s]??'no-entries'; }
  private getMonday() { const d=new Date(),day=d.getDay(),diff=d.getDate()-day+(day===0?-6:1); return new Date(d.setDate(diff)).toISOString().split('T')[0]; }
}
