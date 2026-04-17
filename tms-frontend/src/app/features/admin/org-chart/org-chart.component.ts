import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface OrgNode {
  id: number;
  fullName: string;
  email: string;
  department?: string;
  roles: string[];
  reports: OrgNode[];
}

@Component({
  selector: 'tms-org-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">🌳 Org Chart</h1>
        <p class="page-header__subtitle">Manager-employee hierarchy</p>
      </div>
    </div>

    @if (loading()) {
      <div class="card"><div class="skeleton skeleton--card" style="height:300px"></div></div>
    } @else if (tree().length === 0) {
      <div class="card">
        <div class="empty-state">
          <div class="empty-state__icon">🌳</div>
          <div class="empty-state__title">No org chart data</div>
          <div class="empty-state__description">Assign managers to employees to build the org chart.</div>
        </div>
      </div>
    } @else {
      <div class="card">
        <div class="org-tree">
          @for (node of tree(); track node.id) {
            <ng-container *ngTemplateOutlet="nodeTemplate; context: {node: node, depth: 0}"></ng-container>
          }
        </div>
      </div>
    }

    <ng-template #nodeTemplate let-node="node" let-depth="depth">
      <div class="org-node" [style.margin-left.px]="depth * 40">
        <div class="org-node__card" [class.org-node__card--manager]="hasReports(node)">
          <div class="org-node__avatar">{{ getInitials(node.fullName) }}</div>
          <div class="org-node__info">
            <div class="org-node__name">{{ node.fullName }}</div>
            <div class="org-node__email">{{ node.email }}</div>
            @if (node.department) {
              <div class="org-node__dept">{{ node.department }}</div>
            }
          </div>
          <div class="org-node__roles">
            @for (r of node.roles; track r) {
              <span class="role-chip role-chip--{{ r.toLowerCase() }}">{{ r }}</span>
            }
          </div>
          @if (hasReports(node)) {
            <div class="org-node__count">{{ node.reports.length }} report{{ node.reports.length !== 1 ? 's' : '' }}</div>
          }
        </div>
        @if (node.reports?.length > 0) {
          <div class="org-node__children">
            @for (child of node.reports; track child.id) {
              <ng-container *ngTemplateOutlet="nodeTemplate; context: {node: child, depth: depth + 1}"></ng-container>
            }
          </div>
        }
      </div>
    </ng-template>
  `,
  styles: [`
    .org-tree { padding: 8px 0; }

    .org-node {
      margin-bottom: 8px;
    }

    .org-node__card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px 16px;
      background: var(--surface-secondary);
      border: 1px solid var(--color-neutral-200);
      border-radius: var(--border-radius-md);
      transition: all 150ms;

      &:hover { border-color: var(--color-primary-300); background: var(--color-primary-50); }

      &--manager {
        border-left: 3px solid var(--color-accent-500);
        background: var(--color-accent-100);
        border-color: var(--color-accent-300);
      }
    }

    .org-node__avatar {
      width: 40px; height: 40px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.875rem; color: #fff;
      flex-shrink: 0;
    }

    .org-node__info { flex: 1; min-width: 0; }
    .org-node__name { font-weight: 600; font-size: 0.9375rem; color: var(--color-neutral-900); }
    .org-node__email { font-size: 0.8125rem; color: var(--color-neutral-500); }
    .org-node__dept { font-size: 0.75rem; color: var(--color-neutral-400); margin-top: 1px; }

    .org-node__roles { display: flex; gap: 4px; flex-wrap: wrap; }

    .org-node__count {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--color-accent-700);
      background: var(--color-accent-200);
      padding: 2px 8px;
      border-radius: 10px;
      white-space: nowrap;
    }

    .org-node__children {
      margin-left: 20px;
      padding-left: 20px;
      border-left: 2px dashed var(--color-neutral-300);
      margin-top: 4px;
    }

    .role-chip {
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 0.6875rem;
      font-weight: 700;
      text-transform: uppercase;

      &--employee { background: var(--color-primary-50);  color: var(--color-primary-600); }
      &--manager  { background: var(--color-accent-100);  color: var(--color-accent-700); }
      &--hr       { background: var(--color-info-light);  color: var(--color-info); }
      &--admin    { background: var(--color-danger-light); color: var(--color-danger); }
    }
  `]
})
export class OrgChartComponent implements OnInit {
  private readonly http = inject(HttpClient);
  loading = signal(true);
  tree = signal<OrgNode[]>([]);

  ngOnInit() { this.load(); }

  async load() {
    this.loading.set(true);
    try {
      // Build org tree from users + manager assignments
      const [usersRes, assignmentsRes] = await Promise.all([
        firstValueFrom(this.http.get<any>('/api/admin/users?size=100', { withCredentials: true })),
        firstValueFrom(this.http.get<any[]>('/api/admin/manager-assignments', { withCredentials: true }))
      ]);

      const users: any[] = usersRes.content ?? usersRes ?? [];
      const assignments: any[] = assignmentsRes ?? [];

      // Build tree
      const userMap = new Map(users.map((u: any) => [u.id, { ...u, reports: [] as OrgNode[] }]));

      // Map employee → manager
      const employeeToManager = new Map<number, number>();
      for (const a of assignments) {
        if (a.effectiveTo == null) {
          employeeToManager.set(a.employeeId, a.managerId);
        }
      }

      // Find roots (users with no manager or whose manager is themselves)
      const roots: OrgNode[] = [];
      for (const [id, user] of userMap) {
        const managerId = employeeToManager.get(id);
        if (!managerId || managerId === id) {
          roots.push(user as OrgNode);
        } else {
          const manager = userMap.get(managerId);
          if (manager) {
            (manager as any).reports.push(user);
          } else {
            roots.push(user as OrgNode);
          }
        }
      }

      this.tree.set(roots);
    } catch {
      // Fallback: flat list
      try {
        const res = await firstValueFrom(this.http.get<any>('/api/admin/users?size=100', { withCredentials: true }));
        const users = (res.content ?? res ?? []).map((u: any) => ({ ...u, reports: [] }));
        this.tree.set(users);
      } catch { this.tree.set([]); }
    } finally {
      this.loading.set(false);
    }
  }

  hasReports(node: OrgNode): boolean {
    return node.reports?.length > 0;
  }

  getInitials(name: string): string {
    return (name ?? '').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }
}
