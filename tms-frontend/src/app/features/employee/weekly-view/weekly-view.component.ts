import { Component, inject, signal, computed, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface Entry { id: number; taskName: string; projectName?: string; hours: number; status: string; taskDescription?: string; }
interface DaySummary { date: string; label: string; dayOfWeek: string; entries: Entry[]; totalHours: number; dayStatus: string; isToday: boolean; isWeekend: boolean; isMissed: boolean; isHoliday: boolean; holidayName?: string; isPast: boolean; }

@Component({
  selector: 'tms-weekly-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">📅 Weekly View</h1>
        <p class="page-header__subtitle">{{ weekLabel() }}</p>
      </div>
      <div class="page-header__actions">
        <button class="btn btn--ghost btn--sm" (click)="prevWeek()">← Prev Week</button>
        <button class="btn btn--ghost btn--sm" (click)="goToCurrentWeek()">Today</button>
        <button class="btn btn--ghost btn--sm" [disabled]="isCurrentWeek()" (click)="nextWeek()">Next Week →</button>
      </div>
    </div>

    @if (loading()) {
      <div class="weekly-grid">
        @for (i of [1,2,3,4,5,6,7]; track i) {
          <div class="skeleton" style="height:200px; border-radius:12px;"></div>
        }
      </div>
    } @else {
      <div class="weekly-grid">
        @for (day of days(); track day.date) {
          <div class="day-card"
            [class.day-card--today]="day.isToday"
            [class.day-card--weekend]="day.isWeekend"
            [class.day-card--missed]="day.isMissed"
            [class.day-card--holiday]="day.isHoliday"
            [class.day-card--future]="!day.isPast && !day.isToday">

            <div class="day-card__header">
              <div>
                <div class="day-card__dow">{{ day.dayOfWeek }}</div>
                <div class="day-card__date">{{ day.label }}</div>
              </div>
              @if (day.isHoliday) {
                <span title="{{ day.holidayName }}">🏖️</span>
              } @else if (day.isWeekend) {
                <span class="day-card__weekend-tag">Weekend</span>
              }
            </div>

            @if (day.entries.length > 0) {
              <div class="day-card__status">
                <span class="badge badge--{{ day.dayStatus.toLowerCase().replace('_','-') }}">
                  {{ statusLabel(day.dayStatus) }}
                </span>
                <span class="day-card__hours">{{ day.totalHours | number:'1.1-1' }}h</span>
              </div>
              <div class="day-card__entries">
                @for (entry of day.entries; track entry.id) {
                  <div class="entry-chip" [class]="'entry-chip--' + entry.status.toLowerCase().replace('_','-')">
                    <span class="entry-chip__name">{{ entry.taskName }}</span>
                    <span class="entry-chip__hours">{{ entry.hours }}h</span>
                  </div>
                }
              </div>
            } @else if (day.isMissed) {
              <div class="day-card__missed">
                <p>No entries logged</p>
                <a [routerLink]="['/employee/log-time']" [queryParams]="{date: day.date}" class="btn btn--primary btn--sm">Log Now</a>
              </div>
            } @else if (day.isHoliday) {
              <div class="day-card__holiday-info">
                <p>{{ day.holidayName }}</p>
                <a [routerLink]="['/employee/log-time']" [queryParams]="{date: day.date}" class="btn btn--ghost btn--sm">Log Time</a>
              </div>
            } @else if (!day.isPast && !day.isToday) {
              <div class="day-card__future"><p>Future</p></div>
            } @else {
              <div class="day-card__empty">
                <p>No entries</p>
                <a [routerLink]="['/employee/log-time']" [queryParams]="{date: day.date}" class="btn btn--ghost btn--sm">+ Add</a>
              </div>
            }
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .weekly-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 12px;
      @media (max-width: 1024px) { grid-template-columns: repeat(4, 1fr); }
      @media (max-width: 640px)  { grid-template-columns: repeat(2, 1fr); }
    }
    .day-card { background: var(--surface-elevated); border-radius: var(--border-radius-lg); padding: 16px; box-shadow: var(--shadow-sm); border: 2px solid transparent; min-height: 180px; display: flex; flex-direction: column; gap: 10px; transition: all var(--transition-base);
      &--today { border-color: var(--color-accent-500); box-shadow: var(--shadow-glow-accent); }
      &--missed { background: var(--color-warning-light); border-color: var(--color-warning); }
      &--holiday { background: #f0fdf4; border-color: #86efac; }
      &--weekend { background: var(--color-neutral-50); }
      &--future { opacity: 0.5; }
      &__header { display: flex; justify-content: space-between; align-items: flex-start; }
      &__dow { font-size: 0.75rem; font-weight: 600; color: var(--color-neutral-500); text-transform: uppercase; letter-spacing: 0.06em; }
      &__date { font-family: var(--font-display); font-size: 1.25rem; font-weight: 700; color: var(--color-neutral-900); }
      &__weekend-tag { font-size: 0.6875rem; background: var(--color-neutral-200); color: var(--color-neutral-500); padding: 2px 6px; border-radius: 4px; font-weight: 600; }
      &__status { display: flex; align-items: center; justify-content: space-between; }
      &__hours { font-weight: 700; font-size: 0.9rem; color: var(--color-neutral-700); }
      &__entries { display: flex; flex-direction: column; gap: 4px; }
      &__missed, &__empty, &__future, &__holiday-info { display: flex; flex-direction: column; align-items: center; gap: 8px; flex: 1; justify-content: center; text-align: center; p { font-size: 0.8125rem; color: var(--color-neutral-500); } }
    }
    .entry-chip { display: flex; justify-content: space-between; align-items: center; padding: 4px 8px; border-radius: 6px; font-size: 0.75rem; gap: 4px;
      &--pending { background: var(--color-accent-100); color: var(--color-accent-700); }
      &--approved, &--auto-approved { background: var(--color-success-light); color: var(--color-success); }
      &--rejected { background: var(--color-danger-light); color: var(--color-danger); }
      &--clarification-requested { background: var(--color-info-light); color: var(--color-info); }
      &__name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; font-weight: 500; }
      &__hours { font-weight: 700; flex-shrink: 0; }
    }
  `]
})
export class WeeklyViewComponent implements OnInit {
  private readonly http = inject(HttpClient);

  loading = signal(true);
  weekStart = signal(this.getMonday(new Date()));
  entries = signal<any[]>([]);
  holidays = signal<any[]>([]);

  readonly weekLabel = computed(() => {
    const [sy, sm, sd] = this.weekStart().split('-').map(Number);
    const s = new Date(sy, sm - 1, sd);
    const e = new Date(sy, sm - 1, sd + 6);
    return `${s.toLocaleDateString('en-US',{month:'short',day:'numeric'})} – ${e.toLocaleDateString('en-US',{month:'short',day:'numeric',year:'numeric'})}`;
  });

  readonly days = computed((): DaySummary[] => {
    const start = new Date(this.weekStart() + 'T00:00:00');
    const todayLocal = this.toLocalDateStr(new Date());
    const holidayDates = new Set(this.holidays().map((h: any) => this.normalizeDate(h.date)));
    const holidayMap = new Map(this.holidays().map((h: any) => [this.normalizeDate(h.date), h.name]));

    return Array.from({length: 7}, (_, i) => {
      const d = new Date(start); d.setDate(d.getDate() + i);
      const dateStr = this.toLocalDateStr(d);  // Use LOCAL date, not UTC
      const dow = d.getDay();
      const isWeekend = dow === 0 || dow === 6;
      const isHoliday = holidayDates.has(dateStr);
      const isPast = dateStr < todayLocal;
      const isToday = dateStr === todayLocal;
      const dayEntries = this.entries().filter((e: any) => this.normalizeDate(e.date) === dateStr);
      const totalHours = dayEntries.reduce((s: number, e: any) => s + (e.hours ?? 0), 0);
      const statuses = dayEntries.map((e: any) => e.status);
      const dayStatus = this.computeDayStatus(statuses);
      const isMissed = isPast && !isWeekend && !isHoliday && dayEntries.length === 0;

      return {
        date: dateStr,
        label: d.toLocaleDateString('en-US', {month:'short', day:'numeric'}),
        dayOfWeek: d.toLocaleDateString('en-US', {weekday:'short'}),
        entries: dayEntries.map((e: any) => ({
          id: e.id, taskName: e.taskName, hours: e.hours, status: e.status
        })),
        totalHours, dayStatus, isToday, isWeekend, isMissed, isHoliday,
        holidayName: holidayMap.get(dateStr), isPast: isPast || isToday
      };
    });
  });

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      const [entriesRes, holidaysRes] = await Promise.all([
        firstValueFrom(this.http.get<any[]>(`/api/timesheets/week?weekStart=${this.weekStart()}`, {withCredentials:true})),
        firstValueFrom(this.http.get<any[]>('/api/hr/holidays', {withCredentials:true}))
      ]);
      this.entries.set(entriesRes ?? []);
      this.holidays.set(holidaysRes ?? []);
    } catch { this.entries.set([]); this.holidays.set([]); }
    finally { this.loading.set(false); }
  }

  prevWeek() { const d = new Date(this.weekStart()+'T00:00:00'); d.setDate(d.getDate()-7); this.weekStart.set(this.toLocalDateStr(d)); this.load(); }
  nextWeek() { const d = new Date(this.weekStart()+'T00:00:00'); d.setDate(d.getDate()+7); this.weekStart.set(this.toLocalDateStr(d)); this.load(); }
  goToCurrentWeek() { this.weekStart.set(this.getMonday(new Date())); this.load(); }
  isCurrentWeek() { return this.weekStart() === this.getMonday(new Date()); }

  statusLabel(s: string) { return s.replace('_',' ').replace('CLARIFICATION REQUESTED','CLARIFICATION'); }

  private computeDayStatus(statuses: string[]): string {
    if (!statuses.length) return 'NO_ENTRIES';
    if (statuses.some(s => s === 'REJECTED')) return 'REJECTED';
    if (statuses.some(s => s === 'CLARIFICATION_REQUESTED')) return 'CLARIFICATION_REQUESTED';
    if (statuses.every(s => s === 'APPROVED' || s === 'AUTO_APPROVED')) return 'APPROVED';
    return 'PENDING';
  }

  /** Convert a Date to local YYYY-MM-DD string (no UTC conversion) */
  private toLocalDateStr(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /** Normalize any date string (ISO or plain) to YYYY-MM-DD local */
  private normalizeDate(dateStr: string): string {
    if (!dateStr) return '';
    // If it has a T, it's an ISO string — parse as local by taking just the date part
    // BUT we need to handle UTC offset: "2026-04-14T00:00:00.000Z" in IST is still Apr 14
    // So just take the first 10 chars
    return dateStr.substring(0, 10);
  }

  private getMonday(d: Date): string {
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    const monday = new Date(d);
    monday.setDate(diff);
    return this.toLocalDateStr(monday);
  }
}
