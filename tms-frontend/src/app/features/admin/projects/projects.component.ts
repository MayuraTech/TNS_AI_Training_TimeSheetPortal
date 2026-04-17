import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'tms-projects',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1 class="page-header__title">🗂️ Project Management</h1>
      <div class="page-header__actions">
        <button class="btn btn--primary" (click)="showForm=!showForm">+ New Project</button>
      </div>
    </div>

    @if (showForm) {
      <div class="card mb-4" style="border:2px solid var(--color-accent-500)">
        <h3 style="font-family:var(--font-display);font-weight:600;margin-bottom:16px">New Project</h3>
        <div class="filters-row">
          <div class="form-group" style="flex:2"><label class="form-label">Name *</label><input class="form-control" [(ngModel)]="form.name" /></div>
          <div class="form-group"><label class="form-label">Code *</label><input class="form-control" [(ngModel)]="form.code" placeholder="e.g. TMS-001" /></div>
          <div class="form-group" style="flex:1"><label class="form-label">Client</label><input class="form-control" [(ngModel)]="form.client" /></div>
          <div class="form-group"><label class="form-label">Start Date</label><input type="date" class="form-control" [(ngModel)]="form.startDate" /></div>
        </div>
        <div class="flex gap-3 mt-4">
          <button class="btn btn--primary" (click)="createProject()">Create</button>
          <button class="btn btn--ghost" (click)="showForm=false">Cancel</button>
        </div>
        @if (formError()) { <div class="form-error mt-2">⚠ {{ formError() }}</div> }
      </div>
    }

    <div class="flex gap-3 mb-4">
      <button class="btn btn--sm" [class]="filter==='ACTIVE'?'btn--primary':'btn--ghost'" (click)="filter='ACTIVE';load()">Active</button>
      <button class="btn btn--sm" [class]="filter==='ARCHIVED'?'btn--primary':'btn--ghost'" (click)="filter='ARCHIVED';load()">Archived</button>
      <button class="btn btn--sm" [class]="filter===''?'btn--primary':'btn--ghost'" (click)="filter='';load()">All</button>
    </div>

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead><tr><th>Name</th><th>Code</th><th>Client</th><th>Start Date</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            @for (p of projects(); track p.id) {
              <tr>
                <td><strong>{{ p.name }}</strong></td>
                <td><code style="background:var(--color-neutral-100);padding:2px 6px;border-radius:4px;font-size:0.8125rem">{{ p.code }}</code></td>
                <td>{{ p.client ?? '—' }}</td>
                <td>{{ p.startDate ?? '—' }}</td>
                <td><span class="badge" [class]="p.status==='ACTIVE'?'badge--approved':'badge--no-entries'">{{ p.status }}</span></td>
                <td>
                  @if (p.status === 'ACTIVE') {
                    <button class="btn btn--ghost btn--sm" (click)="archive(p.id)">Archive</button>
                  } @else {
                    <button class="btn btn--success btn--sm" (click)="restore(p.id)">Restore</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`.filters-row{display:flex;gap:16px;flex-wrap:wrap;}`]
})
export class ProjectsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(true);
  projects = signal<any[]>([]);
  filter = 'ACTIVE';
  showForm = false;
  form = { name:'', code:'', client:'', startDate:'' };
  formError = signal<string|null>(null);

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      let url = '/api/admin/projects';
      if (this.filter) url += `?status=${this.filter}`;
      const res = await firstValueFrom(this.http.get<any>(url, {withCredentials:true}));
      this.projects.set(Array.isArray(res) ? res : res.content ?? []);
    } catch {} finally { this.loading.set(false); }
  }

  async createProject() {
    this.formError.set(null);
    if (!this.form.name || !this.form.code) { this.formError.set('Name and code are required'); return; }
    try {
      await firstValueFrom(this.http.post('/api/admin/projects', this.form, {withCredentials:true}));
      this.showForm = false; this.form = {name:'',code:'',client:'',startDate:''}; this.load();
    } catch (e: any) { this.formError.set(e?.error?.error ?? 'Failed to create project'); }
  }

  async archive(id: number) {
    try { await firstValueFrom(this.http.post(`/api/admin/projects/${id}/archive`, {}, {withCredentials:true})); this.load(); } catch {}
  }
  async restore(id: number) {
    try { await firstValueFrom(this.http.post(`/api/admin/projects/${id}/restore`, {}, {withCredentials:true})); this.load(); } catch {}
  }
}
