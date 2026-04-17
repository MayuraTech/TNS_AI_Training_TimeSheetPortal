import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-reminders',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="page-header"><h1 class="page-header__title">🔔 Reminders</h1></div>

    <div class="grid-2">
      <div class="card">
        <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:8px">Missing Entry Reminder</h3>
        <p style="color:var(--color-neutral-500);font-size:0.9rem;margin-bottom:20px">Send a reminder to all employees who have missing timesheet entries for this week.</p>
        <button class="btn btn--primary" [disabled]="sending()" (click)="sendMissing()">
          @if (sending()) { Sending... } @else { 📧 Send to All with Missed Dates }
        </button>
        @if (missingResult()) { <div class="alert-success mt-4">✅ {{ missingResult() }}</div> }
      </div>

      <div class="card">
        <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:8px">Pending Approval Reminder</h3>
        <p style="color:var(--color-neutral-500);font-size:0.9rem;margin-bottom:20px">Send a reminder to managers who have pending approvals older than 2 business days.</p>
        <button class="btn btn--secondary" [disabled]="sending()" (click)="sendPending()">
          @if (sending()) { Sending... } @else { 📧 Send to Managers with Stale Items }
        </button>
        @if (pendingResult()) { <div class="alert-success mt-4">✅ {{ pendingResult() }}</div> }
      </div>
    </div>
  `,
  styles: [`.alert-success{padding:10px 14px;background:var(--color-success-light);color:var(--color-success);border-radius:var(--border-radius-md);font-size:0.875rem;font-weight:500;}`]
})
export class RemindersComponent {
  private readonly http = inject(HttpClient);
  sending = signal(false);
  missingResult = signal<string|null>(null);
  pendingResult = signal<string|null>(null);

  async sendMissing() {
    this.sending.set(true);
    try {
      const r = await firstValueFrom(this.http.post<any>('/api/hr/reminders/missing', {}, {withCredentials:true}));
      this.missingResult.set(r.message ?? `Sent to ${r.recipientCount} employees`);
    } catch { this.missingResult.set('Failed to send reminders'); }
    finally { this.sending.set(false); }
  }

  async sendPending() {
    this.sending.set(true);
    try {
      const r = await firstValueFrom(this.http.post<any>('/api/hr/reminders/pending-approvals', {}, {withCredentials:true}));
      this.pendingResult.set(r.message ?? `Sent to ${r.recipientCount} managers`);
    } catch { this.pendingResult.set('Failed to send reminders'); }
    finally { this.sending.set(false); }
  }
}
