import {
  Component, inject, signal, OnInit, ChangeDetectionStrategy,
  ViewEncapsulation, ElementRef, viewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

const ALL_ROLES = ['EMPLOYEE', 'MANAGER', 'HR', 'ADMIN'] as const;
type Role = typeof ALL_ROLES[number];

@Component({
  selector: 'tms-users',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">👤 User Management</h1>
        <p class="page-header__subtitle">{{ totalUsers() }} users total</p>
      </div>
      <div class="page-header__actions">
        <button class="btn btn--primary" (click)="openCreate()">+ Add User</button>
      </div>
    </div>

    <div class="card mb-4" style="padding:16px 20px">
      <div class="filters-row">
        <div class="form-group" style="flex:2">
          <label class="form-label">Search</label>
          <input class="form-control" [(ngModel)]="search" (input)="load()" placeholder="Name or email..." />
        </div>
        <div class="form-group">
          <label class="form-label">Status</label>
          <select class="form-control" [(ngModel)]="statusFilter" (change)="load()">
            <option value="">All</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Role</label>
          <select class="form-control" [(ngModel)]="roleFilter" (change)="load()">
            <option value="">All Roles</option>
            <option value="EMPLOYEE">Employee</option><option value="MANAGER">Manager</option>
            <option value="HR">HR</option><option value="ADMIN">Admin</option>
          </select>
        </div>
        <button class="btn btn--ghost btn--sm" style="align-self:flex-end;height:44px" (click)="clearFilters()">✕ Clear</button>
      </div>
    </div>

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card"></div></div>
    } @else if (filteredUsers().length === 0) {
      <div class="card"><div class="empty-state"><div class="empty-state__icon">👤</div><div class="empty-state__title">No users found</div></div></div>
    } @else {
      <div class="card" style="padding:0;overflow:hidden">
        <table class="tms-table">
          <thead><tr><th>User</th><th>Department</th><th>Roles</th><th>Status</th><th style="text-align:center">Actions</th></tr></thead>
          <tbody>
            @for (u of filteredUsers(); track u.id) {
              <tr>
                <td>
                  <div class="user-cell">
                    <div class="user-avatar">{{ getInitials(u.fullName) }}</div>
                    <div>
                      <div style="font-weight:600;font-size:.9375rem">{{ u.fullName }}</div>
                      <div style="font-size:.8125rem;color:var(--color-neutral-500)">{{ u.email }}</div>
                      @if (u.employeeId) { <div style="font-size:.75rem;color:var(--color-neutral-400)">ID: {{ u.employeeId }}</div> }
                    </div>
                  </div>
                </td>
                <td>{{ u.department ?? '—' }}</td>
                <td>
                  <div class="role-chips">
                    @for (r of u.roles; track r) { <span class="role-chip role-chip--{{ r.toLowerCase() }}">{{ r }}</span> }
                  </div>
                </td>
                <td><span class="badge" [class]="u.status==='ACTIVE'?'badge--approved':'badge--rejected'">{{ u.status }}</span></td>
                <td>
                  <div class="flex gap-2" style="justify-content:center">
                    <button class="btn btn--secondary btn--sm" (click)="openEdit(u)">✏️ Edit</button>
                    @if (u.status==='ACTIVE') {
                      <button class="btn btn--danger btn--sm" (click)="deactivate(u.id)">🚫</button>
                    } @else {
                      <button class="btn btn--success btn--sm" (click)="reactivate(u.id)">✅</button>
                    }
                    <button class="btn btn--ghost btn--sm" (click)="resetPassword(u.id)">🔑</button>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }

    <!-- Native <dialog> — guaranteed true popup above all content -->
    <dialog #dlg class="tms-dlg">
      <div class="tms-dlg__wrap">
        <div class="tms-dlg__head">
          <h3>{{ editingUser() ? 'Edit User' : 'Create New User' }}</h3>
          <button class="tms-dlg__x" (click)="closeModal()">✕</button>
        </div>

        @if (modalError()) {
          <div class="tms-dlg__err">⚠️ {{ modalError() }}</div>
        }

        <div class="tms-dlg__body">
          <p class="tms-dlg__sec">Basic Information</p>
          <div class="tms-dlg__row">
            <div class="form-group">
              <label class="form-label">Full Name <span style="color:var(--color-danger)">*</span></label>
              <input class="form-control" [(ngModel)]="form.fullName" placeholder="e.g. John Doe" />
            </div>
            <div class="form-group">
              <label class="form-label">Email <span style="color:var(--color-danger)">*</span></label>
              <input type="email" class="form-control" [(ngModel)]="form.email" placeholder="john@company.com" />
            </div>
          </div>
          <div class="tms-dlg__row">
            <div class="form-group">
              <label class="form-label">Department</label>
              <input class="form-control" [(ngModel)]="form.department" placeholder="e.g. Engineering" />
            </div>
            <div class="form-group">
              <label class="form-label">Employee ID</label>
              <input class="form-control" [(ngModel)]="form.employeeId" placeholder="e.g. EMP001" />
            </div>
          </div>

          <p class="tms-dlg__sec" style="margin-top:20px">Roles <span style="color:var(--color-danger)">*</span></p>
          <p style="font-size:.8125rem;color:var(--color-neutral-500);margin-bottom:12px">Click to toggle roles. EMPLOYEE is the baseline.</p>
          <div class="role-grid">
            @for (role of allRoles; track role) {
              <div class="role-tile" [class.role-tile--on]="form.roles.includes(role)" (click)="toggleRole(role)">
                <span style="font-size:1.375rem">{{ roleIcon(role) }}</span>
                <div style="flex:1">
                  <strong style="display:block;font-size:.875rem;color:var(--color-neutral-800)">{{ role }}</strong>
                  <span style="font-size:.75rem;color:var(--color-neutral-500)">{{ roleDesc(role) }}</span>
                </div>
                <span class="role-tile__chk">✓</span>
              </div>
            }
          </div>
          @if (form.roles.length === 0) {
            <p class="form-error" style="margin-top:8px">⚠ Select at least one role</p>
          }
        </div>

        <div class="tms-dlg__foot">
          <button class="btn btn--ghost" (click)="closeModal()">Cancel</button>
          <button class="btn btn--primary"
            [disabled]="saving() || !form.fullName.trim() || !form.email.trim() || form.roles.length===0"
            (click)="saveUser()">
            @if (saving()) { <span class="spin"></span> Saving... }
            @else { {{ editingUser() ? 'Save Changes' : 'Create User' }} }
          </button>
        </div>
      </div>
    </dialog>
  `,
  styles: [`
    .filters-row { display:flex; gap:12px; flex-wrap:wrap; align-items:flex-end; }
    .user-cell { display:flex; align-items:center; gap:12px; }
    .user-avatar {
      width:38px; height:38px; border-radius:50%;
      background:linear-gradient(135deg,var(--color-primary-500),var(--color-primary-400));
      display:flex; align-items:center; justify-content:center;
      font-weight:700; font-size:.875rem; color:#fff; flex-shrink:0;
    }
    .role-chips { display:flex; gap:4px; flex-wrap:wrap; }
    .role-chip { padding:2px 7px; border-radius:4px; font-size:.6875rem; font-weight:700; text-transform:uppercase; }
    .role-chip--employee { background:var(--color-primary-50); color:var(--color-primary-600); }
    .role-chip--manager  { background:var(--color-accent-100); color:var(--color-accent-700); }
    .role-chip--hr       { background:var(--color-info-light); color:var(--color-info); }
    .role-chip--admin    { background:var(--color-danger-light); color:var(--color-danger); }

    /* Native dialog */
    .tms-dlg {
      border:none; padding:0; margin:auto;
      border-radius:16px;
      width:min(620px,95vw);
      max-height:90vh;
      box-shadow:0 24px 64px rgba(10,22,40,.4);
      overflow:hidden;
      &::backdrop { background:rgba(10,22,40,.65); backdrop-filter:blur(4px); }
      &[open] { animation:dlgPop 200ms ease-out; }
    }
    @keyframes dlgPop {
      from { opacity:0; transform:scale(.95) translateY(8px); }
      to   { opacity:1; transform:scale(1) translateY(0); }
    }

    .tms-dlg__wrap { display:flex; flex-direction:column; max-height:90vh; background:var(--surface-elevated); }
    .tms-dlg__head {
      display:flex; align-items:center; justify-content:space-between;
      padding:18px 24px; border-bottom:1px solid var(--color-neutral-200);
      h3 { font-family:var(--font-display); font-size:1.125rem; font-weight:700; margin:0; }
    }
    .tms-dlg__x {
      width:32px; height:32px; border-radius:8px; border:none; background:none;
      cursor:pointer; font-size:1rem; color:var(--color-neutral-500);
      display:flex; align-items:center; justify-content:center;
      &:hover { background:var(--color-neutral-100); }
    }
    .tms-dlg__err {
      margin:12px 24px 0; padding:10px 14px;
      background:var(--color-danger-light); color:var(--color-danger);
      border-radius:8px; font-size:.875rem; font-weight:500;
    }
    .tms-dlg__body { padding:20px 24px; overflow-y:auto; flex:1; }
    .tms-dlg__sec { font-size:.75rem; font-weight:700; color:var(--color-neutral-500); text-transform:uppercase; letter-spacing:.08em; margin-bottom:12px; }
    .tms-dlg__row { display:grid; grid-template-columns:1fr 1fr; gap:16px; margin-bottom:16px; }
    .tms-dlg__foot { padding:16px 24px; border-top:1px solid var(--color-neutral-200); display:flex; justify-content:flex-end; gap:12px; }

    .role-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
    .role-tile {
      display:flex; align-items:center; gap:12px; padding:12px 14px;
      border:2px solid var(--color-neutral-200); border-radius:10px;
      cursor:pointer; background:var(--surface-secondary); transition:all 150ms; user-select:none;
    }
    .role-tile:hover { border-color:var(--color-primary-300); background:var(--color-primary-50); }
    .role-tile.role-tile--on { border-color:var(--color-accent-500); background:var(--color-accent-100); }
    .role-tile.role-tile--on .role-tile__chk { opacity:1; }
    .role-tile__chk {
      width:22px; height:22px; border-radius:50%;
      background:var(--color-accent-500); color:var(--color-neutral-900);
      display:flex; align-items:center; justify-content:center;
      font-size:.75rem; font-weight:800; flex-shrink:0; opacity:0; transition:opacity 150ms;
    }
    .spin {
      width:14px; height:14px; border:2px solid rgba(0,0,0,.2); border-top-color:#000;
      border-radius:50%; animation:spin .7s linear infinite; display:inline-block;
    }
    @keyframes spin { to { transform:rotate(360deg); } }
  `]
})
export class UsersComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly allRoles: Role[] = ['EMPLOYEE', 'MANAGER', 'HR', 'ADMIN'];

  private dlgRef = viewChild<ElementRef<HTMLDialogElement>>('dlg');

  loading = signal(true);
  saving = signal(false);
  users = signal<any[]>([]);
  totalUsers = signal(0);
  editingUser = signal<any | null>(null);
  modalError = signal<string | null>(null);

  search = ''; statusFilter = ''; roleFilter = '';
  form = this.emptyForm();

  readonly filteredUsers = () => {
    let list = this.users();
    if (this.roleFilter) list = list.filter((u: any) => u.roles?.includes(this.roleFilter));
    return list;
  };

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      let url = '/api/admin/users?size=100';
      if (this.search)       url += `&search=${encodeURIComponent(this.search)}`;
      if (this.statusFilter) url += `&status=${this.statusFilter}`;
      const res = await firstValueFrom(this.http.get<any>(url, { withCredentials: true }));
      const list = res.content ?? res ?? [];
      this.users.set(list);
      this.totalUsers.set(res.totalElements ?? list.length);
    } catch { this.users.set([]); } finally { this.loading.set(false); }
  }

  clearFilters() { this.search = ''; this.statusFilter = ''; this.roleFilter = ''; this.load(); }

  openCreate() {
    this.editingUser.set(null);
    this.form = this.emptyForm();
    this.modalError.set(null);
    this.show();
  }

  openEdit(user: any) {
    this.editingUser.set(user);
    this.form = {
      fullName: user.fullName ?? '', email: user.email ?? '',
      department: user.department ?? '', employeeId: user.employeeId ?? '',
      roles: [...(user.roles ?? ['EMPLOYEE'])]
    };
    this.modalError.set(null);
    this.show();
  }

  closeModal() { this.dlgRef()?.nativeElement?.close(); }

  private show() {
    const el = this.dlgRef()?.nativeElement;
    if (el && !el.open) el.showModal();
  }

  toggleRole(role: Role) {
    const idx = this.form.roles.indexOf(role);
    if (idx >= 0) {
      if (this.form.roles.length === 1) return;
      this.form.roles = this.form.roles.filter(r => r !== role);
    } else {
      this.form.roles = [...this.form.roles, role];
    }
  }

  async saveUser() {
    if (!this.form.fullName.trim() || !this.form.email.trim() || this.form.roles.length === 0) return;
    this.saving.set(true);
    this.modalError.set(null);
    try {
      if (this.editingUser()) {
        await firstValueFrom(this.http.put(
          `/api/admin/users/${this.editingUser().id}`,
          { fullName: this.form.fullName, email: this.form.email, department: this.form.department, employeeId: this.form.employeeId, roles: this.form.roles },
          { withCredentials: true }
        ));
      } else {
        await firstValueFrom(this.http.post('/api/admin/users',
          { fullName: this.form.fullName, email: this.form.email, department: this.form.department, employeeId: this.form.employeeId, roles: this.form.roles },
          { withCredentials: true }
        ));
      }
      this.closeModal();
      this.load();
    } catch (err: any) {
      this.modalError.set(err?.error?.error ?? 'Failed to save. Please try again.');
    } finally { this.saving.set(false); }
  }

  async deactivate(id: number) {
    if (!confirm('Deactivate this user?')) return;
    try { await firstValueFrom(this.http.post(`/api/admin/users/${id}/deactivate`, {}, { withCredentials: true })); this.load(); } catch {}
  }
  async reactivate(id: number) {
    try { await firstValueFrom(this.http.post(`/api/admin/users/${id}/reactivate`, {}, { withCredentials: true })); this.load(); } catch {}
  }
  async resetPassword(id: number) {
    if (!confirm('Send password reset email?')) return;
    try { await firstValueFrom(this.http.post(`/api/admin/users/${id}/reset-password`, {}, { withCredentials: true })); alert('✅ Reset email sent!'); } catch {}
  }

  getInitials(name: string) { return (name ?? '').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2); }
  roleIcon(r: Role) { return { EMPLOYEE: '👤', MANAGER: '👥', HR: '🏢', ADMIN: '⚙️' }[r]; }
  roleDesc(r: Role) { return { EMPLOYEE: 'Log time, view history', MANAGER: 'Approve team timesheets', HR: 'Reports & reminders', ADMIN: 'Full system access' }[r]; }
  private emptyForm() { return { fullName: '', email: '', department: '', employeeId: '', roles: ['EMPLOYEE'] as Role[] }; }
}
