import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-hr-reports',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header"><h1 class="page-header__title">📊 HR Reports</h1></div>

    <div class="card mb-4">
      <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:16px">Generate Report</h3>
      <div class="filters-row">
        <div class="form-group" style="flex:2">
          <label class="form-label">Report Type</label>
          <select class="form-control" [(ngModel)]="reportType">
            <option value="WEEKLY_COMPLIANCE">Weekly Compliance Report</option>
            <option value="MONTHLY_HOURS_SUMMARY">Monthly Hours Summary</option>
            <option value="PROJECT_HOURS_DIST">Project Hours Distribution</option>
            <option value="EMPLOYEE_ATTENDANCE">Employee Attendance Overview</option>
          </select>
        </div>
      </div>
      <button class="btn btn--primary mt-4" [disabled]="generating()" (click)="generate()">
        @if (generating()) { <span class="spinner-sm"></span> Generating... } @else { 🚀 Generate Report }
      </button>
    </div>

    @if (jobs().length > 0) {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead><tr><th>Report Type</th><th>Status</th><th>Created</th><th>Action</th></tr></thead>
          <tbody>
            @for (job of jobs(); track job.id) {
              <tr>
                <td>{{ job.reportType.replace('_',' ') }}</td>
                <td><span class="badge badge--{{ statusClass(job.status) }}">{{ job.status }}</span></td>
                <td>{{ formatDate(job.createdAt) }}</td>
                <td>
                  @if (job.status === 'COMPLETED') {
                    <a [href]="job.filePath" class="btn btn--sm btn--secondary">⬇ Download</a>
                  } @else if (job.status === 'PENDING' || job.status === 'PROCESSING') {
                    <button class="btn btn--sm btn--ghost" (click)="pollStatus(job.id)">🔄 Refresh</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`.filters-row{display:flex;gap:16px;}.spinner-sm{width:14px;height:14px;border:2px solid rgba(0,0,0,0.2);border-top-color:#000;border-radius:50%;animation:spin 0.7s linear infinite;display:inline-block;}@keyframes spin{to{transform:rotate(360deg);}}`]
})
export class HrReportsComponent {
  private readonly http = inject(HttpClient);
  generating = signal(false);
  jobs = signal<any[]>([]);
  reportType = 'WEEKLY_COMPLIANCE';

  async generate() {
    this.generating.set(true);
    try {
      const job = await firstValueFrom(this.http.post<any>('/api/hr/reports/generate', {reportType: this.reportType}, {withCredentials:true}));
      this.jobs.update(j => [job, ...j]);
      setTimeout(() => this.pollStatus(job.id), 2000);
    } catch {} finally { this.generating.set(false); }
  }

  async pollStatus(id: number) {
    try {
      const job = await firstValueFrom(this.http.get<any>(`/api/hr/reports/exports/${id}/status`, {withCredentials:true}));
      this.jobs.update(j => j.map(x => x.id === id ? job : x));
    } catch {}
  }

  formatDate(d: string) { return new Date(d).toLocaleString(); }
  statusClass(s: string) { return s==='COMPLETED'?'approved':s==='FAILED'?'rejected':'pending'; }
}
