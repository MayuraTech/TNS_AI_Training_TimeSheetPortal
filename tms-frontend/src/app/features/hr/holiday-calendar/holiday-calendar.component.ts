import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'tms-holiday-calendar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1 class="page-header__title">🏖️ Holiday Calendar</h1>
      @if (canEdit()) {
        <div class="page-header__actions">
          <button class="btn btn--primary" (click)="showForm=true">+ Add Holiday</button>
        </div>
      }
    </div>

    @if (showForm) {
      <div class="card mb-4" style="border:2px solid var(--color-accent-500)">
        <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:16px">Add Holiday</h3>
        <div class="filters-row">
          <div class="form-group" style="flex:2"><label class="form-label">Name *</label><input class="form-control" [(ngModel)]="form.name" placeholder="e.g. Diwali" /></div>
          <div class="form-group"><label class="form-label">Date *</label><input type="date" class="form-control" [(ngModel)]="form.date" /></div>
          <div class="form-group"><label class="form-label">Type</label>
            <select class="form-control" [(ngModel)]="form.type">
              <option value="PUBLIC">Public Holiday</option>
              <option value="COMPANY">Company Holiday</option>
              <option value="OPTIONAL">Optional</option>
            </select>
          </div>
        </div>
        <div class="flex gap-3 mt-4">
          <button class="btn btn--primary" (click)="addHoliday()">Save</button>
          <button class="btn btn--ghost" (click)="showForm=false">Cancel</button>
        </div>
      </div>
    }

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else if (holidays().length === 0) {
      <div class="card"><div class="empty-state"><div class="empty-state__icon">🏖️</div><div class="empty-state__title">No holidays configured</div></div></div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead><tr><th>Name</th><th>Date</th><th>Type</th><th>Applicable To</th>@if(canEdit()){<th>Actions</th>}</tr></thead>
          <tbody>
            @for (h of holidays(); track h.id) {
              <tr>
                <td><strong>{{ h.name }}</strong></td>
                <td>{{ formatDate(h.date) }}</td>
                <td><span class="badge" [class]="typeClass(h.type)">{{ h.type }}</span></td>
                <td>{{ h.applicableTo }}</td>
                @if (canEdit()) {
                  <td><button class="btn btn--danger btn--sm" (click)="deleteHoliday(h.id)">🗑</button></td>
                }
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`.filters-row{display:flex;gap:16px;flex-wrap:wrap;}`]
})
export class HolidayCalendarComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  loading = signal(true);
  holidays = signal<any[]>([]);
  showForm = false;
  form = { name: '', date: '', type: 'PUBLIC', applicableTo: 'ALL' };

  canEdit() { return this.auth.hasRole('HR') || this.auth.hasRole('ADMIN'); }

  ngOnInit() { this.load(); }
  async load() {
    try { this.holidays.set(await firstValueFrom(this.http.get<any[]>('/api/hr/holidays', {withCredentials:true})) ?? []); }
    catch {} finally { this.loading.set(false); }
  }

  async addHoliday() {
    if (!this.form.name || !this.form.date) return;
    try {
      await firstValueFrom(this.http.post('/api/hr/holidays', this.form, {withCredentials:true}));
      this.showForm = false; this.form = {name:'',date:'',type:'PUBLIC',applicableTo:'ALL'}; this.load();
    } catch (e: any) { alert(e?.error?.error ?? 'Failed to add holiday'); }
  }

  async deleteHoliday(id: number) {
    if (!confirm('Delete this holiday?')) return;
    try { await firstValueFrom(this.http.delete(`/api/hr/holidays/${id}`, {withCredentials:true})); this.load(); } catch {}
  }

  formatDate(d: string) {
    const [y, m, day] = d.substring(0, 10).split('-').map(Number);
    return new Date(y, m - 1, day).toLocaleDateString('en-US', { weekday: 'short', month: 'long', day: 'numeric', year: 'numeric' });
  }
  typeClass(t: string) { return t==='PUBLIC'?'badge--approved':t==='COMPANY'?'badge--pending':'badge--no-entries'; }
}
