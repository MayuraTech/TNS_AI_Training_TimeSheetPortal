import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-system-config',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1 class="page-header__title">⚙️ System Configuration</h1>
      <div class="page-header__actions">
        <button class="btn btn--primary" [disabled]="saving()" (click)="saveAll()">
          @if (saving()) { Saving... } @else { 💾 Save Changes }
        </button>
      </div>
    </div>

    @if (saved()) { <div class="alert-success mb-4">✅ Configuration saved successfully. Changes take effect immediately.</div> }

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else {
      <div class="grid-2">
        <div class="card">
          <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:20px">Work Hours</h3>
          <div class="config-list">
            <div class="config-item">
              <label class="form-label">Daily Warning Threshold (hrs)</label>
              <input type="number" class="form-control" [(ngModel)]="config['daily_hours_warning_threshold']" step="0.5" min="1" max="24" />
              <p class="config-hint">Show warning banner when daily total exceeds this value</p>
            </div>
            <div class="config-item">
              <label class="form-label">Overtime Threshold (hrs)</label>
              <input type="number" class="form-control" [(ngModel)]="config['daily_hours_overtime_threshold']" step="0.5" min="1" max="24" />
              <p class="config-hint">Require justification when daily total exceeds this value</p>
            </div>
            <div class="config-item">
              <label class="form-label">Past Entry Edit Window (days)</label>
              <input type="number" class="form-control" [(ngModel)]="config['past_entry_edit_window_days']" min="1" max="365" />
              <p class="config-hint">How many days back employees can log/edit time</p>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:20px">Reminders & Schedule</h3>
          <div class="config-list">
            <div class="config-item">
              <label class="form-label">Reminder Schedule Time</label>
              <input type="time" class="form-control" [(ngModel)]="config['reminder_schedule_time']" />
              <p class="config-hint">Daily time to send automated reminders</p>
            </div>
            <div class="config-item">
              <label class="form-label">Reminder Days</label>
              <input class="form-control" [(ngModel)]="config['reminder_schedule_days']" placeholder="MON,TUE,WED,THU,FRI" />
              <p class="config-hint">Comma-separated days (MON,TUE,WED,THU,FRI)</p>
            </div>
            <div class="config-item">
              <label class="form-label">Work Week Days</label>
              <input class="form-control" [(ngModel)]="config['work_week_days']" />
              <p class="config-hint">Standard work days for compliance calculation</p>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:20px">Email (SMTP)</h3>
          <div class="config-list">
            <div class="config-item"><label class="form-label">SMTP Host</label><input class="form-control" [(ngModel)]="config['smtp_host']" /></div>
            <div class="config-item"><label class="form-label">SMTP Port</label><input type="number" class="form-control" [(ngModel)]="config['smtp_port']" /></div>
            <div class="config-item"><label class="form-label">From Address</label><input type="email" class="form-control" [(ngModel)]="config['smtp_from_address']" /></div>
            <div class="config-item"><label class="form-label">From Name</label><input class="form-control" [(ngModel)]="config['smtp_from_name']" /></div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`.config-list{display:flex;flex-direction:column;gap:16px;}.config-item{display:flex;flex-direction:column;gap:4px;}.config-hint{font-size:0.75rem;color:var(--color-neutral-400);margin-top:2px;}.alert-success{padding:12px 16px;background:var(--color-success-light);color:var(--color-success);border-radius:var(--border-radius-md);font-weight:500;}`]
})
export class SystemConfigComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(true);
  saving = signal(false);
  saved = signal(false);
  config: Record<string, string> = {};

  ngOnInit() { this.load(); }

  async load() {
    try { this.config = await firstValueFrom(this.http.get<Record<string,string>>('/api/admin/config', {withCredentials:true})) ?? {}; }
    catch {} finally { this.loading.set(false); }
  }

  async saveAll() {
    this.saving.set(true); this.saved.set(false);
    try {
      await firstValueFrom(this.http.put('/api/admin/config', this.config, {withCredentials:true}));
      this.saved.set(true);
      setTimeout(() => this.saved.set(false), 3000);
    } catch {} finally { this.saving.set(false); }
  }
}
