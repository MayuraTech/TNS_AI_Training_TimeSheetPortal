import {
  Component, inject, signal, computed, OnInit, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

interface Project { id: number; name: string; code: string; }
interface TaskEntry { projectId: number | null; taskName: string; taskDescription: string; hours: number; }

@Component({
  selector: 'tms-log-time',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="log-time-page">

      <!-- Page Header -->
      <div class="page-header">
        <div>
          <h1 class="page-header__title">⏱ Log Time</h1>
          <p class="page-header__subtitle">
            Logging for: <strong>{{ formattedDate() }}</strong>
            <span class="tz-badge">{{ timezone() }}</span>
          </p>
        </div>
      </div>

      <!-- Date Selector -->
      <div class="card mb-4">
        <div class="date-selector">
          <button class="btn btn--ghost btn--sm" (click)="changeDate(-1)">← Prev</button>
          <div class="date-display">
            <input
              type="date"
              class="form-control date-input"
              [value]="selectedDate()"
              [max]="today()"
              (change)="onDateChange($event)"
            />
          </div>
          <button class="btn btn--ghost btn--sm"
            [disabled]="selectedDate() === today()"
            (click)="changeDate(1)">Next →</button>
        </div>

        <!-- Daily total bar -->
        <div class="daily-total-bar" [class.warning]="dailyTotal() > 8" [class.overtime]="dailyTotal() > 9">
          <div class="daily-total-bar__info">
            <span class="daily-total-bar__label">Daily Total</span>
            <span class="daily-total-bar__value">{{ dailyTotal() | number:'1.1-1' }} hrs</span>
          </div>
          <div class="daily-total-bar__track">
            <div class="daily-total-bar__fill"
              [style.width.%]="Math.min((dailyTotal() / 9) * 100, 100)">
            </div>
          </div>
          @if (dailyTotal() > 8 && dailyTotal() <= 9) {
            <div class="alert alert--warning">
              ⚠️ You've logged more than 8 hours today.
            </div>
          }
          @if (dailyTotal() > 9) {
            <div class="alert alert--danger">
              🚨 Daily total exceeds 9 hours. Please provide an overtime justification below.
            </div>
          }
        </div>
      </div>

      <!-- Task Entries -->
      <div class="tasks-section">
        <div class="tasks-header">
          <h3>Tasks</h3>
          <button class="btn btn--secondary btn--sm" (click)="addTask()">+ Add Task</button>
        </div>

        @for (task of tasks(); track $index; let i = $index) {
          <div class="task-card card mb-4">
            <div class="task-card__header">
              <span class="task-card__number">Task {{ i + 1 }}</span>
              @if (tasks().length > 1) {
                <button class="btn btn--ghost btn--sm task-remove" (click)="removeTask(i)">✕ Remove</button>
              }
            </div>

            <div class="task-form">
              <!-- Project -->
              <div class="form-group">
                <label class="form-label">Project <span class="required">*</span></label>
                @if (loadingProjects()) {
                  <div class="skeleton skeleton--text"></div>
                } @else {
                  <select class="form-control"
                    [(ngModel)]="task.projectId"
                    [name]="'project_' + i"
                    [class.form-control--error]="submitted() && !task.projectId">
                    <option [ngValue]="null" disabled>Select a project...</option>
                    @for (p of projects(); track p.id) {
                      <option [ngValue]="p.id">{{ p.name }} ({{ p.code }})</option>
                    }
                  </select>
                  @if (submitted() && !task.projectId) {
                    <span class="form-error">⚠ Project is required</span>
                  }
                }
              </div>

              <!-- Task Name + Hours row -->
              <div class="task-form__row">
                <div class="form-group" style="flex:2">
                  <label class="form-label">Task Name <span class="required">*</span></label>
                  <input type="text" class="form-control"
                    [(ngModel)]="task.taskName"
                    [name]="'taskName_' + i"
                    placeholder="e.g. Backend API Development"
                    maxlength="100"
                    [class.form-control--error]="submitted() && !task.taskName.trim()" />
                  @if (submitted() && !task.taskName.trim()) {
                    <span class="form-error">⚠ Task name is required</span>
                  }
                </div>

                <div class="form-group hours-group">
                  <label class="form-label">Hours <span class="required">*</span></label>
                  <div class="hours-stepper">
                    <button type="button" class="hours-btn" (click)="decrementHours(i)"
                      [disabled]="task.hours <= 0.5">−</button>
                    <span class="hours-value">{{ task.hours | number:'1.1-1' }}</span>
                    <button type="button" class="hours-btn" (click)="incrementHours(i)"
                      [disabled]="task.hours >= 9">+</button>
                  </div>
                  <div class="hours-hint">0.5 – 9.0 hrs</div>
                </div>
              </div>

              <!-- Description -->
              <div class="form-group">
                <label class="form-label">Description <span class="optional">(optional)</span></label>
                <textarea class="form-control"
                  [(ngModel)]="task.taskDescription"
                  [name]="'desc_' + i"
                  placeholder="Brief description of what you worked on..."
                  rows="2"
                  maxlength="500"></textarea>
                <div class="char-count">{{ task.taskDescription.length }}/500</div>
              </div>
            </div>
          </div>
        }

        <!-- Overtime Justification -->
        @if (dailyTotal() > 9) {
          <div class="card mb-4 overtime-card">
            <div class="form-group">
              <label class="form-label">
                🚨 Overtime Justification <span class="required">*</span>
              </label>
              <textarea class="form-control"
                [(ngModel)]="overtimeJustification"
                placeholder="Please explain why you logged more than 9 hours today... (min 10 characters)"
                rows="3"
                maxlength="300"
                [class.form-control--error]="submitted() && dailyTotal() > 9 && overtimeJustification.trim().length < 10">
              </textarea>
              <div class="char-count" [class.error]="overtimeJustification.trim().length < 10">
                {{ overtimeJustification.trim().length }}/300 (min 10)
              </div>
              @if (submitted() && dailyTotal() > 9 && overtimeJustification.trim().length < 10) {
                <span class="form-error">⚠ Please provide a reason for logging more than 9 hours.</span>
              }
            </div>
          </div>
        }

        <!-- Submit -->
        <div class="submit-bar">
          <div class="submit-bar__summary">
            <span>{{ tasks().length }} task(s)</span>
            <span class="dot">·</span>
            <span class="submit-bar__total" [class.overtime]="dailyTotal() > 9">
              {{ dailyTotal() | number:'1.1-1' }} hrs total
            </span>
          </div>
          <button class="btn btn--primary btn--lg"
            (click)="submitAll()"
            [disabled]="submitting()">
            @if (submitting()) {
              <span class="spinner-sm"></span> Submitting...
            } @else {
              Submit {{ tasks().length }} Task(s)
            }
          </button>
        </div>
      </div>

      <!-- Success Toast -->
      @if (successMsg()) {
        <div class="toast toast--success">
          ✅ {{ successMsg() }}
        </div>
      }

      <!-- Error Toast -->
      @if (errorMsg()) {
        <div class="toast toast--error">
          ❌ {{ errorMsg() }}
        </div>
      }
    </div>
  `,
  styles: [`
    .log-time-page { max-width: 100%; }

    .tz-badge {
      display: inline-block;
      margin-left: 8px;
      padding: 2px 8px;
      background: var(--color-primary-50);
      color: var(--color-primary-600);
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 500;
    }

    .date-selector {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 20px;
    }

    .date-input {
      width: 180px;
      text-align: center;
      font-weight: 600;
    }

    .daily-total-bar {
      &__info {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
      }
      &__label { font-size: 0.875rem; color: var(--color-neutral-500); font-weight: 500; }
      &__value { font-weight: 700; font-size: 1rem; color: var(--color-neutral-800); }
      &__track {
        height: 8px;
        background: var(--color-neutral-200);
        border-radius: 4px;
        overflow: hidden;
        margin-bottom: 12px;
      }
      &__fill {
        height: 100%;
        background: var(--color-primary-400);
        border-radius: 4px;
        transition: width 0.3s ease, background 0.3s ease;
      }

      &.warning .daily-total-bar__fill { background: var(--color-warning); }
      &.overtime .daily-total-bar__fill { background: var(--color-danger); }
      &.overtime .daily-total-bar__value { color: var(--color-danger); }
    }

    .alert {
      padding: 10px 14px;
      border-radius: var(--border-radius-md);
      font-size: 0.875rem;
      font-weight: 500;
      margin-top: 8px;

      &--warning { background: var(--color-warning-light); color: var(--color-warning); border: 1px solid rgba(217,119,6,0.2); }
      &--danger  { background: var(--color-danger-light);  color: var(--color-danger);  border: 1px solid rgba(220,38,38,0.2); }
    }

    .tasks-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      h3 { font-family: var(--font-display); font-size: 1.125rem; font-weight: 600; }
    }

    .task-card {
      border: 1px solid var(--color-neutral-200);
      transition: border-color var(--transition-fast);
      &:hover { border-color: var(--color-primary-200); }

      &__header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid var(--color-neutral-100);
      }

      &__number {
        font-weight: 600;
        font-size: 0.875rem;
        color: var(--color-primary-500);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
    }

    .task-remove { color: var(--color-danger); }

    .task-form {
      display: flex;
      flex-direction: column;
      gap: 16px;

      &__row {
        display: flex;
        gap: 16px;
        align-items: flex-start;

        @media (max-width: 600px) { flex-direction: column; }
      }
    }

    .hours-group { flex-shrink: 0; width: 140px; }

    .hours-stepper {
      display: flex;
      align-items: center;
      border: 1px solid var(--color-neutral-300);
      border-radius: var(--border-radius-md);
      overflow: hidden;
      height: 44px;
    }

    .hours-btn {
      width: 40px;
      height: 100%;
      background: var(--color-neutral-100);
      border: none;
      font-size: 1.25rem;
      font-weight: 600;
      cursor: pointer;
      color: var(--color-neutral-700);
      transition: background var(--transition-fast);

      &:hover:not(:disabled) { background: var(--color-primary-50); color: var(--color-primary-600); }
      &:disabled { opacity: 0.4; cursor: not-allowed; }
    }

    .hours-value {
      flex: 1;
      text-align: center;
      font-weight: 700;
      font-size: 1rem;
      color: var(--color-neutral-900);
    }

    .hours-hint { font-size: 0.75rem; color: var(--color-neutral-400); margin-top: 4px; text-align: center; }

    .char-count {
      font-size: 0.75rem;
      color: var(--color-neutral-400);
      text-align: right;
      margin-top: 4px;
      &.error { color: var(--color-danger); }
    }

    .required { color: var(--color-danger); margin-left: 2px; }
    .optional { color: var(--color-neutral-400); font-weight: 400; font-size: 0.8125rem; }

    .overtime-card {
      border: 2px solid var(--color-danger);
      background: var(--color-danger-light);
    }

    .submit-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20px 24px;
      background: var(--surface-elevated);
      border-radius: var(--border-radius-lg);
      box-shadow: var(--shadow-md);
      border: 1px solid var(--color-neutral-200);

      &__summary {
        display: flex;
        align-items: center;
        gap: 8px;
        color: var(--color-neutral-600);
        font-size: 0.9375rem;
      }

      &__total {
        font-weight: 700;
        color: var(--color-neutral-900);
        &.overtime { color: var(--color-danger); }
      }

      .dot { color: var(--color-neutral-300); }
    }

    .spinner-sm {
      width: 14px; height: 14px;
      border: 2px solid rgba(0,0,0,0.2);
      border-top-color: #000;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .toast {
      position: fixed;
      bottom: 32px;
      right: 32px;
      padding: 14px 20px;
      border-radius: var(--border-radius-md);
      font-weight: 600;
      font-size: 0.9375rem;
      box-shadow: var(--shadow-xl);
      z-index: 1000;
      animation: slideUp 0.3s ease-out;

      &--success { background: var(--color-success); color: #fff; }
      &--error   { background: var(--color-danger);  color: #fff; }
    }
  `]
})
export class LogTimeComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly Math = Math;

  projects = signal<Project[]>([]);
  loadingProjects = signal(true);
  submitting = signal(false);
  submitted = signal(false);
  successMsg = signal<string | null>(null);
  errorMsg = signal<string | null>(null);

  selectedDate = signal(this.todayStr());
  overtimeJustification = '';

  tasks = signal<TaskEntry[]>([this.emptyTask()]);

  readonly timezone = computed(() => this.authService.user()?.timezone ?? 'UTC');

  readonly dailyTotal = computed(() =>
    this.tasks().reduce((sum, t) => sum + t.hours, 0)
  );

  readonly formattedDate = computed(() => {
    const d = new Date(this.selectedDate() + 'T00:00:00');
    return d.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
  });

  ngOnInit(): void {
    // Check for date query param (from "Log Now" links)
    this.route.queryParams.subscribe(params => {
      if (params['date']) this.selectedDate.set(params['date']);
    });
    this.loadProjects();
  }

  private async loadProjects(): Promise<void> {
    try {
      const data = await firstValueFrom(
        this.http.get<Project[]>('/api/projects/active', { withCredentials: true })
      );
      this.projects.set(Array.isArray(data) ? data : []);
    } catch {
      this.projects.set([]);
    } finally {
      this.loadingProjects.set(false);
    }
  }

  addTask(): void {
    this.tasks.update(t => [...t, this.emptyTask()]);
  }

  removeTask(index: number): void {
    this.tasks.update(t => t.filter((_, i) => i !== index));
  }

  incrementHours(index: number): void {
    this.tasks.update(tasks => tasks.map((t, i) =>
      i === index ? { ...t, hours: Math.min(+(t.hours + 0.5).toFixed(1), 9) } : t
    ));
  }

  decrementHours(index: number): void {
    this.tasks.update(tasks => tasks.map((t, i) =>
      i === index ? { ...t, hours: Math.max(+(t.hours - 0.5).toFixed(1), 0.5) } : t
    ));
  }

  onDateChange(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    if (val) this.selectedDate.set(val);
  }

  changeDate(delta: number): void {
    const d = new Date(this.selectedDate() + 'T00:00:00');
    d.setDate(d.getDate() + delta);
    const newDate = d.toISOString().split('T')[0];
    if (newDate <= this.todayStr()) this.selectedDate.set(newDate);
  }

  async submitAll(): Promise<void> {
    this.submitted.set(true);

    // Validate
    const invalid = this.tasks().some(t => !t.projectId || !t.taskName.trim());
    if (invalid) return;
    if (this.dailyTotal() > 9 && this.overtimeJustification.trim().length < 10) return;

    this.submitting.set(true);
    this.errorMsg.set(null);

    let successCount = 0;
    let autoApprovedCount = 0;

    for (const task of this.tasks()) {
      try {
        const payload: any = {
          projectId: task.projectId,
          date: this.selectedDate(),
          taskName: task.taskName.trim(),
          taskDescription: task.taskDescription.trim() || null,
          hours: task.hours,
          overtimeJustification: this.dailyTotal() > 9 ? this.overtimeJustification.trim() : null
        };
        const result = await firstValueFrom(
          this.http.post<any>('/api/timesheets/entries', payload, { withCredentials: true })
        );
        successCount++;
        if (result.status === 'AUTO_APPROVED') autoApprovedCount++;
      } catch (err: any) {
        this.errorMsg.set(err?.error?.error ?? 'Failed to submit entry. Please try again.');
        this.submitting.set(false);
        return;
      }
    }

    this.submitting.set(false);
    this.submitted.set(false);

    let msg = `${successCount} task(s) submitted successfully!`;
    if (autoApprovedCount > 0) msg += ` (${autoApprovedCount} auto-approved ✓)`;
    this.successMsg.set(msg);

    // Reset form
    this.tasks.set([this.emptyTask()]);
    this.overtimeJustification = '';

    setTimeout(() => this.successMsg.set(null), 4000);
  }

  today(): string { return this.todayStr(); }

  private todayStr(): string {
    return new Date().toISOString().split('T')[0];
  }

  private emptyTask(): TaskEntry {
    return { projectId: null, taskName: '', taskDescription: '', hours: 4.0 };
  }
}
