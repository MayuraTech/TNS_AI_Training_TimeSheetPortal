import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-manager-assignments',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">👥 Manager Assignments</h1>
        <p class="page-header__subtitle">Assign managers to employees</p>
      </div>
    </div>

    @if (successMsg()) {
      <div class="alert-success mb-4">✅ {{ successMsg() }}</div>
    }
    @if (errorMsg()) {
      <div class="alert-error mb-4">⚠️ {{ errorMsg() }}</div>
    }

    <!-- Assign form -->
    <div class="card mb-4">
      <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:16px">Assign / Reassign Manager</h3>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">Employee <span style="color:var(--color-danger)">*</span></label>
          <select class="form-control" [(ngModel)]="form.employeeId">
            <option [ngValue]="null">Select employee...</option>
            @for (u of employees(); track u.id) {
              <option [ngValue]="u.id">{{ u.fullName }} ({{ u.email }})</option>
            }
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Manager <span style="color:var(--color-danger)">*</span></label>
          <select class="form-control" [(ngModel)]="form.managerId">
            <option [ngValue]="null">Select manager...</option>
            @for (u of managers(); track u.id) {
              <option [ngValue]="u.id" [disabled]="u.id === form.employeeId">
                {{ u.fullName }} ({{ u.email }})
              </option>
            }
          </select>
        </div>
        <div style="align-self:flex-end">
          <button
            class="btn btn--primary"
            [disabled]="saving() || !form.employeeId || !form.managerId"
            (click)="assign()"
          >
            @if (saving()) { Saving... } @else { Assign Manager }
          </button>
        </div>
      </div>
    </div>

    <!-- Current assignments -->
    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else if (assignments().length === 0) {
      <div class="card">
        <div class="empty-state">
          <div class="empty-state__icon">👥</div>
          <div class="empty-state__title">No assignments yet</div>
        </div>
      </div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead>
            <tr><th>Employee</th><th>Manager</th><th>Since</th><th>Status</th></tr>
          </thead>
          <tbody>
            @for (a of assignments(); track a.id) {
              <tr>
                <td>
                  <div style="font-weight:600">{{ a.employeeName }}</div>
                  <div style="font-size:0.8125rem;color:var(--color-neutral-500)">{{ a.employeeEmail }}</div>
                </td>
                <td>
                  <div style="font-weight:600">{{ a.managerName }}</div>
                  <div style="font-size:0.8125rem;color:var(--color-neutral-500)">{{ a.managerEmail }}</div>
                </td>
                <td style="font-size:0.875rem">{{ formatDate(a.effectiveFrom) }}</td>
                <td>
                  @if (!a.effectiveTo) {
                    <span class="badge badge--approved">Active</span>
                  } @else {
                    <span class="badge badge--no-entries">Ended {{ formatDate(a.effectiveTo) }}</span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`
    .form-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: 16px; align-items: flex-end; }
    .alert-success { padding: 10px 14px; background: var(--color-success-light); color: var(--color-success); border-radius: var(--border-radius-md); font-weight: 500; }
    .alert-error   { padding: 10px 14px; background: var(--color-danger-light);  color: var(--color-danger);  border-radius: var(--border-radius-md); font-weight: 500; }
  `]
})
export class ManagerAssignmentsComponent implements OnInit {
  private readonly http = inject(HttpClient);

  loading = signal(true);
  saving = signal(false);
  assignments = signal<any[]>([]);
  employees = signal<any[]>([]);
  managers = signal<any[]>([]);
  successMsg = signal<string | null>(null);
  errorMsg = signal<string | null>(null);

  form = { employeeId: null as number | null, managerId: null as number | null };

  ngOnInit() { this.loadAll(); }

  async loadAll() {
    this.loading.set(true);
    try {
      const [usersRes, assignmentsRes] = await Promise.all([
        firstValueFrom(this.http.get<any>('/api/admin/users?size=100', { withCredentials: true })),
        firstValueFrom(this.http.get<any[]>('/api/admin/manager-assignments', { withCredentials: true }))
      ]);

      const users = usersRes.content ?? usersRes ?? [];
      this.employees.set(users.filter((u: any) => u.status === 'ACTIVE'));
      this.managers.set(users.filter((u: any) =>
        u.status === 'ACTIVE' && (u.roles?.includes('MANAGER') || u.roles?.includes('ADMIN'))
      ));
      this.assignments.set(assignmentsRes ?? []);
    } catch { } finally { this.loading.set(false); }
  }

  async assign() {
    if (!this.form.employeeId || !this.form.managerId) return;
    if (this.form.employeeId === this.form.managerId) {
      this.errorMsg.set('An employee cannot be their own manager.');
      return;
    }

    this.saving.set(true);
    this.errorMsg.set(null);
    this.successMsg.set(null);

    try {
      await firstValueFrom(this.http.post('/api/admin/manager-assignments', {
        employeeId: this.form.employeeId,
        managerId:  this.form.managerId
      }, { withCredentials: true }));

      this.successMsg.set('Manager assigned successfully!');
      this.form = { employeeId: null, managerId: null };
      setTimeout(() => this.successMsg.set(null), 3000);
      this.loadAll();
    } catch (err: any) {
      this.errorMsg.set(err?.error?.error ?? 'Failed to assign manager. Check for circular assignments.');
    } finally {
      this.saving.set(false);
    }
  }

  formatDate(d: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
