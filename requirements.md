# Requirements Document

## Timesheet Management System (TMS)

**Version:** 1.6  
**Date:** 2025  
**Organization:** Think N Solutions  
**Tech Stack:** Java 21 · Spring Boot (latest) · Angular 21 · MS SQL Server

---

## Introduction

The Timesheet Management System (TMS) is a web-based application that enables employees to log their daily work hours against projects and tasks, and allows managers to review and approve those entries. HR and Admin roles provide oversight, reporting, and system configuration capabilities.

The system prioritizes daily logging discipline, transparent approval workflows, and rich visibility into how time is distributed across projects and teams. Every user — regardless of role — is also an employee and has full access to log their own time. Role elevation grants additional capabilities on top of the base employee experience.

The UI is built on the TNS design system: deep navy sidebar, amber accent highlights, card-based layouts with subtle elevation, and smooth Angular animations throughout.

---

## Glossary

- **TMS**: Timesheet Management System — the application described in this document.
- **Employee**: Any registered user of the system. All roles inherit employee capabilities.
- **Manager**: An employee with authority to review and approve/reject timesheet entries for assigned direct reports.
- **HR**: A user with read access to aggregated timesheet summaries and the ability to send reminders.
- **Admin**: A user with full system configuration access including user, project, and assignment management.
- **Timesheet_Entry**: A single record of hours worked by an employee on a specific task on a specific date, linked to a project and containing a free-text task description. Time is logged at the task level, not the day level.
- **Work_Day**: A calendar day (Monday–Friday) on which time logging is expected. Employees may optionally log time on weekends; weekend entries are treated as overtime and visually distinguished.
- **Work_Week**: A Monday-to-Sunday period used as the primary navigation unit in the UI; weekdays (Mon–Fri) are standard work days and Saturday–Sunday are optional overtime days.
- **Daily_Summary**: An aggregated view of all Timesheet_Entries for a single employee on a single calendar day, including total hours and a computed Day_Status.
- **Weekly_Summary**: An aggregated view of all Daily_Summaries for a single employee across one Work_Week.
- **Approval_Status**: The current state of a Timesheet_Entry (task-level). Valid values: PENDING, APPROVED, REJECTED, CLARIFICATION_REQUESTED.
- **Day_Status**: A computed status derived from all task-level entries for a given day. Rules: ALL APPROVED → APPROVED; ANY REJECTED → REJECTED; ANY CLARIFICATION_REQUESTED (and none REJECTED) → CLARIFICATION_REQUESTED; otherwise → PENDING.
- **Missed_Date**: A Work_Day (Mon–Fri) on which an employee has no submitted Timesheet_Entry by end of business. Weekend days and Organisation_Holiday days are never flagged as missed.
- **Organisation_Holiday**: A calendar date designated as a company-wide non-working day, configured by HR or Admin. Holidays are excluded from missed-date detection, automated reminders, and compliance calculations.
- **Project**: A named entity representing a client engagement, internal initiative, or overhead category against which time can be logged.
- **Manager_Assignment**: A mapping record that associates one Employee with one Manager.
- **Audit_Log**: An immutable record of every state-changing action performed in the system.
- **Notification**: An email or in-app alert sent to a user in response to a system event.
- **Clarification_Thread**: A conversation thread attached to a Timesheet_Entry or Daily_Summary used to exchange comments between employee and manager.
- **Dashboard**: The landing page for each role, presenting a personalized summary of timesheet status.
- **Reminder**: A scheduled or manually triggered Notification prompting action on pending or missing timesheets.
- **Skeleton_Screen**: A loading placeholder that mirrors the shape of the content being loaded, used to reduce perceived latency.
- **JWT**: JSON Web Token used for stateless authentication between Angular frontend and Spring Boot backend.
- **RBAC**: Role-Based Access Control — the permission model governing what each role can see and do.

---

## Requirements

### 1. Authentication & Authorization

#### 1.1 User Login

**User Story:** As a user, I want to log in with my company credentials so that I can access the TMS securely.

**Acceptance Criteria:**
- AC1: The login page displays an email and password field with a "Sign In" button.
- AC2: On successful authentication, the system issues a JWT and redirects the user to their role-specific dashboard.
- AC3: On failed authentication, the system displays an inline error message without revealing whether the email or password was incorrect.
- AC4: After 5 consecutive failed attempts, the account is temporarily locked for 15 minutes and the user is notified via email.
- AC5: JWT tokens expire after 8 hours; the system silently refreshes the token if the user is active.
- AC6: On logout, the JWT is invalidated server-side and the user is redirected to the login page.

#### 1.2 Role-Based Access Control

**User Story:** As an admin, I want each user to see only what their role permits so that data is protected and the UI is uncluttered.

**Acceptance Criteria:**
- AC1: Every user inherits the Employee role as a baseline; Manager, HR, and Admin roles add capabilities on top.
- AC2: A user can hold multiple roles simultaneously (e.g., a Manager who is also an Admin).
- AC3: **[CHANGED v1.2]** Navigation menu items, sidebar sections, dashboard widgets, and action buttons are rendered exclusively based on the user's active roles. Sections and routes that are not applicable to the user's role are **hidden entirely** from the UI — they are never shown as disabled or locked. Direct URL access to a hidden route silently redirects to the user's role-appropriate dashboard rather than showing an error page.
- AC4: API endpoints enforce RBAC server-side regardless of UI state. A role-unauthorized API call returns HTTP 403, but this is a backend safety net only — the UI should never surface this to the user because the triggering element will not be rendered.
- AC5: Role assignments are managed exclusively by Admin users.
- AC6: The UI adapts per role as follows:

| Role | Landing Page | Sidebar Sections Visible |
|---|---|---|
| Employee | My Dashboard | My Dashboard, Log Time, My Timesheets, My Profile |
| Manager | Manager Dashboard | All Employee sections + Team Review, Team Reports |
| HR | HR Dashboard | All Employee sections + HR Overview, Reminders, HR Reports |
| Admin | Admin Dashboard | All sections + User Mgmt, Project Mgmt, System Config, Audit Log |

- AC7: When a user holds multiple roles, a role-switcher control in the top header allows switching the active role context without logging out; the sidebar and dashboard update instantly on switch.

#### 1.3 Password Management

**User Story:** As a user, I want to reset my password securely so that I can regain access if I forget it.

**Acceptance Criteria:**
- AC1: A "Forgot Password" link on the login page triggers a password-reset email with a time-limited token (valid 1 hour).
- AC2: The reset link is single-use; reuse returns an error page.
- AC3: New passwords must be at least 8 characters and include at least one uppercase letter, one number, and one special character.
- AC4: On first login, the user is forced to change the system-generated password before accessing any other page.

---

### 2. Employee — Time Logging

#### 2.1 Task-Level Time Entry

**User Story:** As an employee, I want to log my work hours at the task level for today so that my manager can review and approve them with full context.

**Acceptance Criteria:**
- AC1: The "Log Time" page defaults to today's date (in the user's local timezone) and cannot be set to a future date.
- AC2: Each entry is a task-level record requiring: Project (dropdown, mandatory), Task Name (free text, max 100 characters, mandatory), Task Description (free text, max 500 characters, optional), Hours (numeric, 0.5–9 in 0.5 increments, mandatory).
- AC3: Multiple task entries can be added for the same day (e.g., Task A: 4 hrs on Project A, Task B: 3 hrs on Project B).
- AC4: ~~The total hours logged for a single day are displayed in real time as entries are added; a warning is shown if total exceeds 24 hours.~~ **[CHANGED v1.1]** The total hours logged for a single day are displayed in real time as task entries are added. A warning banner is shown if the daily total exceeds 8 hours. **[CHANGED v1.3]** If the daily total exceeds 9 hours, a prominent warning is shown and the employee is required to enter an overtime justification comment (free text, min 10 characters, max 300 characters) before submission is allowed. Submission without the comment is blocked with an inline message: "Please provide a reason for logging more than 9 hours." There is no hard cap — submission is permitted once the comment is provided.
- AC5: Entries are saved as PENDING on submission. **[NEW v1.5]** Exception: if a task entry has hours < 1 (i.e., 0.5 hours), it is automatically approved by the system immediately on submission without requiring manager action. Auto-approved entries are labelled "Auto-Approved" in the UI and audit log. The manager is not notified for auto-approved entries.
- AC6: The system prevents submission if any mandatory field is empty, showing inline validation messages.
- AC7: A success toast notification confirms submission.
- AC8: Employees cannot log time for dates more than 30 days in the past without Admin override.
- AC9: **[NEW v1.3, CHANGED v1.4]** When a submission's daily total exceeds 9 hours, the overtime justification comment is stored against the day's entry. In the manager's team review view, the affected day column is visually flagged with an amber ⚠️ overtime badge. When the manager expands the day, each task entry that contributed to the overtime total is highlighted with an amber left-border accent and the overtime justification comment is displayed inline beneath the task list for that day. No email or notification panel alert is sent for this event — the signal is surfaced entirely at the task/day level within the review UI.

#### 2.2 Edit Time Entry

**User Story:** As an employee, I want to edit a previously submitted entry so that I can correct mistakes.

**Acceptance Criteria:**
- AC1: Entries in PENDING or CLARIFICATION_REQUESTED status can be edited by the employee.
- AC2: APPROVED or REJECTED entries cannot be edited; the edit button is disabled with a tooltip explaining why.
- AC3: Editing an entry resets its status to PENDING and notifies the assigned manager.
- AC4: All changes are recorded in the Audit_Log with the previous and new values.

#### 2.3 Delete Time Entry

**User Story:** As an employee, I want to delete a PENDING entry so that I can remove an entry logged by mistake.

**Acceptance Criteria:**
- AC1: Only PENDING entries can be deleted by the employee.
- AC2: A confirmation dialog is shown before deletion.
- AC3: Deletion is recorded in the Audit_Log.

#### 2.4 Weekly Time View

**User Story:** As an employee, I want to see my current work week at a glance so that I know which days are complete, pending, or missing.

**Acceptance Criteria:**
- AC1: **[CHANGED v1.1]** The weekly view displays all 7 days of the current week (Monday–Sunday) as columns. Monday–Friday are standard work days; Saturday and Sunday are shown as optional overtime days with a distinct "Weekend" label and a muted background.
- AC2: Each day column shows: total hours logged, Day_Status badge (color-coded — green: APPROVED, amber: PENDING, red: REJECTED, blue: CLARIFICATION_REQUESTED, grey: no entries), and a quick-add task button.
- AC3: Navigation arrows allow moving to previous or future weeks; future weeks beyond the current week are read-only placeholders.
- AC4: Missed dates (past Mon–Fri Work_Days with no entries) are highlighted in amber with a "Log Now" CTA. Weekend days and Organisation_Holiday days with no entries are never flagged as missed. **[CHANGED v1.6]** Holiday dates are shown with a distinct background and 🏖️ icon. A "Log Time" button is still available on holidays for employees who choose to work — but no "Log Now" missed-date CTA is shown.
- AC5: The current day is visually distinguished with a highlighted border.
- AC6: **[NEW v1.1]** Day_Status is computed from task-level statuses using the following precedence rules:
  - ALL tasks APPROVED → Day_Status = APPROVED
  - ANY task REJECTED → Day_Status = REJECTED (regardless of other task statuses)
  - ANY task CLARIFICATION_REQUESTED and none REJECTED → Day_Status = CLARIFICATION_REQUESTED
  - Otherwise → Day_Status = PENDING
- AC7: **[NEW v1.1]** When a day has mixed statuses (e.g., some tasks APPROVED, one REJECTED), the day column shows a split status indicator listing each task's individual status on hover/expand, so the employee understands exactly which tasks need attention.

#### 2.5 Employee Dashboard

**User Story:** As an employee, I want a summary dashboard so that I can quickly understand my timesheet health.

**Acceptance Criteria:**
- AC1: Dashboard displays four KPI cards: Total Hours This Week, Pending Entries, Approved Entries, Missed Dates.
- AC2: A status breakdown donut chart shows the proportion of PENDING / APPROVED / REJECTED entries for the current month.
- AC3: A "Recent Activity" feed shows the last 10 actions (submissions, approvals, rejections) with timestamps.
- AC4: Missed dates are listed with direct "Log Now" links.
- AC5: All dashboard data loads within 2 seconds; skeleton screens are shown during loading.

#### 2.6 Filter & Search

**User Story:** As an employee, I want to filter my timesheet history so that I can find specific entries quickly.

**Acceptance Criteria:**
- AC1: Filters available: Date Range (date picker), Project (multi-select dropdown), Status (multi-select: PENDING, APPROVED, REJECTED, CLARIFICATION_REQUESTED).
- AC2: Filters are applied instantly (no page reload); results update as filter values change.
- AC3: Active filters are shown as removable chips above the results table.
- AC4: A "Clear All Filters" button resets all filters at once.
- AC5: Filtered results can be exported to CSV.

#### 2.7 Universal Read Access to Own Data **[NEW v1.6]**

**User Story:** As an employee, I want to view all my timesheet entries at any time regardless of their status so that I always have full visibility of my own data.

**Acceptance Criteria:**
- AC1: An employee can view all their own Timesheet_Entries at any point in time — including PENDING, APPROVED, REJECTED, CLARIFICATION_REQUESTED, and Auto-Approved entries — with no restrictions based on status or date.
- AC2: Entries that are locked for editing (e.g., APPROVED, or beyond the edit window) remain fully visible in read-only mode. The edit and delete controls are hidden or disabled, but the entry data is always displayed.
- AC3: The clarification thread on any entry is always readable by the employee, even after the entry is APPROVED or REJECTED and the thread is closed for new messages.
- AC4: Managers can always view all task entries for their direct reports regardless of status, including historical entries from before a manager reassignment (read-only).
- AC5: HR can always view aggregated daily summaries for all employees regardless of status or date range.
- AC6: Admin can always view all entries for all users across all statuses and date ranges.
- AC7: The "My Timesheets → History" view has no default date restriction; it loads the last 3 months by default but allows the user to extend the range to their full history.

---

### 3. Manager — Review & Approval

#### 3.1 Manager Dashboard

**User Story:** As a manager, I want a dashboard showing my team's timesheet status so that I can prioritize my review work.

**Acceptance Criteria:**
- AC1: Dashboard shows KPI cards: Total Direct Reports, Pending Approvals (count), Missed Entries (count), Approved This Week.
- AC2: A team summary table lists each direct report with columns: Name, Hours This Week, Pending Count, Missed Count, Last Submission Date.
- AC3: Rows with pending items are sorted to the top by default.
- AC4: Clicking a row drills down to that employee's detail view.
- AC5: Manager also sees their own employee dashboard section (since every manager is also an employee).

#### 3.2 Team Timesheet Review

**User Story:** As a manager, I want to drill into an employee's timesheet so that I can review entries at day and task level.

**Acceptance Criteria:**
- AC1: The employee detail view shows a weekly calendar (Mon–Sun) with daily totals and Day_Status badges.
- AC2: Clicking a day expands to show all task-level Timesheet_Entries for that day with: Task Name, Project, Hours, Task Description, and individual task status.
- AC3: The manager can approve or reject at the day level (bulk action affecting all PENDING tasks for that day) or at the individual task level.
- AC4: Rejection requires a mandatory reason (free text, min 10 characters).
- AC5: "Ask Clarification" sets the individual task entry status to CLARIFICATION_REQUESTED and opens a Clarification_Thread on that task.
- AC6: Bulk approve/reject is available for all pending entries across the week with a single action.
- AC7: **[NEW v1.1]** When a day contains a mix of APPROVED and REJECTED/CLARIFICATION_REQUESTED tasks, the day-level status badge reflects the computed Day_Status (REJECTED takes precedence). The manager can still act on individual tasks independently.

#### 3.3 Approval Actions

**User Story:** As a manager, I want to approve or reject timesheet entries so that employees are accountable for their logged time.

**Acceptance Criteria:**
- AC1: Approving an entry sets its status to APPROVED and sends a notification to the employee.
- AC2: Rejecting an entry sets its status to REJECTED, records the reason, and sends a notification to the employee.
- AC3: A manager cannot approve their own timesheet entries; those are routed to the manager's own manager or HR.
- AC4: All approval actions are recorded in the Audit_Log with actor, timestamp, and reason (if applicable).
- AC5: Once approved, an entry cannot be rejected without Admin intervention.
- AC6: **[NEW v1.5]** Auto-approved entries (hours < 1) are treated as APPROVED for all Day_Status computation purposes. If all remaining tasks on a day are also APPROVED or auto-approved, the Day_Status resolves to APPROVED without any manager action.
- AC7: **[EDGE CASE v1.5]** If an employee edits an auto-approved entry (e.g., increases hours to ≥ 1), the entry status resets to PENDING and enters the normal manager approval workflow. The auto-approval label is removed.

#### 3.4 Clarification Thread

**User Story:** As a manager, I want to request clarification on an entry so that I can get more context before approving or rejecting.

**Acceptance Criteria:**
- AC1: Clicking "Ask Clarification" opens an inline thread panel on the entry.
- AC2: Both manager and employee can post messages in the thread.
- AC3: Each message shows the author's name, role badge, and timestamp.
- AC4: The employee receives an email notification when a clarification is requested.
- AC5: The manager receives an email notification when the employee responds.
- AC6: After clarification, the manager can approve or reject the entry from within the thread panel.
- AC7: The thread is read-only once the entry is APPROVED or REJECTED.

#### 3.5 Manager Filters & Export

**User Story:** As a manager, I want to filter and export team timesheet data so that I can report on team productivity.

**Acceptance Criteria:**
- AC1: Filters available: Employee (multi-select), Date Range, Project (multi-select), Status.
- AC2: Export to CSV and PDF with the current filter applied.
- AC3: PDF export includes the company logo, report title, date range, and a summary table.

---

### 4. HR Features

#### 4.1 HR Dashboard

**User Story:** As an HR user, I want an organization-wide summary so that I can monitor overall timesheet compliance.

**Acceptance Criteria:**
- AC1: Dashboard shows org-wide KPIs: Total Employees, Compliance Rate (% with no missed dates this week), Pending Approvals Org-Wide, Average Hours Per Employee This Week.
- AC2: A department/team breakdown table shows compliance metrics per team.
- AC3: HR cannot see individual task descriptions — only aggregated hours per employee per day.
- AC4: HR can drill down to an employee's daily summary (hours per day) but not to task-level detail.

#### 4.2 Reminders

**User Story:** As an HR user or Manager, I want to send reminders so that employees and managers take timely action.

**Acceptance Criteria:**
- AC1: HR can send a "Missing Entry" reminder to all employees with missed dates for the current week.
- AC2: HR can send a "Pending Approval" reminder to all managers with pending items older than 2 business days.
- AC3: **[NEW v1.1]** Managers can send a "Missing Entry" reminder to their own direct reports who have missed dates for the current week, directly from the Manager Dashboard.
- AC4: **[NEW v1.1]** Managers can send a "Pending Submission" reminder to a specific employee from that employee's detail view.
- AC5: Reminders are sent via email; a preview of the email content is shown before sending.
- AC6: Reminder sends are logged with timestamp and sender identity.
- AC7: Automated reminders run on a configurable schedule (default: daily at 5 PM on Work_Days).
- ~~AC6: Employees and managers can opt out of automated reminders (but not manual HR reminders).~~ **[REMOVED v1.1]** Opt-out for automated reminders is not supported; all users receive automated reminders as configured by Admin.

#### 4.3 HR Reports

**User Story:** As an HR user, I want to generate reports so that I can share timesheet data with leadership.

**Acceptance Criteria:**
- AC1: Available reports: Weekly Compliance Report, Monthly Hours Summary, Project Hours Distribution, Employee Attendance Overview.
- AC2: Each report supports date range selection and export to CSV and PDF.
- AC3: Reports are generated asynchronously for large datasets; the user is notified when ready.

#### 4.4 Organisation Holiday Calendar **[NEW v1.5]**

**User Story:** As an HR user or Admin, I want to manage the organisation's holiday calendar so that public and company holidays are excluded from missed-date calculations and reminders.

**Acceptance Criteria:**
- AC1: Only users with the HR or Admin role can create, edit, or delete holiday entries. Employees and Managers have read-only access to the holiday calendar.
- AC2: A holiday entry requires: Holiday Name (free text, mandatory), Date (mandatory), Type (Public Holiday / Company Holiday / Optional Holiday), Applicable To (All / Specific Department(s)).
- AC3: The holiday calendar is visible to all users as a read-only reference in the "My Timesheets" section, with holidays clearly marked on the weekly view (distinct background colour and a 🏖️ icon).
- AC4: **[CHANGED v1.6]** Holiday dates are never flagged as missed dates and never trigger automated reminders — regardless of whether the employee logged time or not. Employees may still log time on holidays; the day simply carries no compliance obligation.
- AC5: Holiday dates are excluded from compliance rate calculations in HR reports and dashboards.
- AC6: **[CHANGED v1.6]** Employees can voluntarily log time on a holiday using the same task-level entry form. Holiday entries follow the same PENDING → APPROVED/REJECTED workflow as weekend entries and are treated as overtime. Logging on a holiday does not affect the holiday status of that day for other employees.
- AC7: Holiday dates are excluded from compliance rate calculations in HR reports and dashboards regardless of whether the employee logged time or not.
- AC7: Holidays can be configured for the current and next calendar year; bulk import via CSV is supported.
- AC8: Changes to the holiday calendar are recorded in the Audit_Log.
- AC9: **[EDGE CASE]** If a holiday is added retroactively for a date that was already flagged as missed, the system automatically clears the missed-date flag and cancels any pending reminders for that date.
- AC10: **[EDGE CASE]** If a holiday is deleted, dates that fall back into the standard work week are re-evaluated for missed-date status from the next scheduled reminder cycle — not retroactively.

---

### 5. Admin Features

#### 5.1 User Management

**User Story:** As an admin, I want to manage user accounts so that the system reflects the current state of the organization.

**Acceptance Criteria:**
- AC1: Admin can create, edit, deactivate, and reactivate user accounts.
- AC2: User creation form requires: Full Name, Email, Role(s), Manager Assignment (if role includes Employee), Department, Employee ID.
- AC3: Deactivated users cannot log in; their historical data is preserved.
- AC4: Admin can reset any user's password, triggering a password-reset email.
- AC5: Bulk user import via CSV with a defined template; import errors are reported row by row.
- AC6: User list supports search by name/email and filter by role/status/department.

#### 5.2 Project Management

**User Story:** As an admin, I want to manage projects so that employees have an accurate list to log time against.

**Acceptance Criteria:**
- AC1: Admin can create, edit, archive, and restore projects.
- AC2: Project creation requires: Project Name (unique), Project Code (unique, alphanumeric), Client/Department, Start Date, End Date (optional), Status (Active/Archived).
- AC3: Archived projects do not appear in the employee's project dropdown but their historical entries are preserved.
- AC4: Admin can assign employees to projects; only assigned employees see the project in their dropdown.
- AC5: Project list supports search and filter by status/client.

#### 5.3 Manager Assignment

**User Story:** As an admin, I want to assign managers to employees so that approval routing works correctly.

**Acceptance Criteria:**
- AC1: Each employee must have exactly one manager assigned at any time.
- AC2: Admin can reassign a manager; the change takes effect immediately for new entries.
- AC3: Entries submitted before a reassignment retain the original manager for approval.
- AC4: The system prevents circular assignments (e.g., A manages B, B manages A).
- AC5: Admin can view a full org chart of manager-employee relationships.

#### 5.4 System Configuration

**User Story:** As an admin, I want to configure system-wide settings so that the TMS matches our company's policies.

**Acceptance Criteria:**
- AC1: Configurable settings include: Work week definition (default Mon–Fri, with optional weekend logging toggle), Daily hours warning threshold (default 8 hours), Daily hours overtime threshold requiring justification comment (default 9 hours), Reminder schedule (time and days), Past-entry edit window (default 30 days), Lock period (prevent edits to entries older than N days after approval).
- AC2: Changes to settings are logged in the Audit_Log.
- AC3: Settings changes take effect immediately without requiring a system restart.

#### 5.5 Audit Log Viewer

**User Story:** As an admin, I want to view the audit log so that I can investigate any disputes or anomalies.

**Acceptance Criteria:**
- AC1: Audit log displays: Timestamp, Actor (name + role), Action Type, Entity Affected, Before Value, After Value.
- AC2: Filterable by date range, actor, action type, and entity type.
- AC3: Audit log is read-only and cannot be modified or deleted by any user including Admin.
- AC4: Audit log entries are retained for a minimum of 2 years.
- AC5: Export to CSV available.

---

### 6. Notifications

#### 6.1 Email Notifications

**User Story:** As a user, I want to receive email notifications for relevant events so that I stay informed without having to check the app constantly.

**Acceptance Criteria:**
- AC1: Notification triggers and recipients:

| Event | Recipient |
|---|---|
| Task entry submitted | Manager |
| Task entry approved | Employee |
| Task entry rejected | Employee |
| Clarification requested on task | Employee |
| Employee replies to clarification | Manager |
| Task entry edited (was approved) | Manager |
| Missed date detected (EOD, Mon–Fri only, excluding holidays) | Employee + Manager |
| Pending approval > 2 business days | Manager |
| Password reset requested | User |
| Account locked | User |
| New user created | New user (welcome email) |
| Reminder sent by HR | Target recipients |
| **[NEW v1.1]** Reminder sent by Manager | Target employee(s) |
| **[CHANGED v1.4]** Overtime submission (daily total > 9 hrs) | Manager — task-level highlight in review UI only (no email, no notification panel) |
| Weekend entry submitted | Manager (informational, no action required) |

- AC2: All emails use a consistent branded template with the company logo, clear subject line, and a direct deep-link to the relevant item in the app.
- AC3: Emails include a one-click unsubscribe link for automated notifications only.
- AC4: Failed email deliveries are retried up to 3 times with exponential backoff and logged.

#### 6.2 In-App Notifications

**User Story:** As a user, I want in-app notifications so that I see alerts while I'm actively using the system.

**Acceptance Criteria:**
- AC1: A bell icon in the top navigation bar shows an unread count badge.
- AC2: Clicking the bell opens a notification panel showing the last 20 notifications with timestamp and a brief description.
- AC3: Notifications are marked as read when clicked; a "Mark all as read" option is available.
- AC4: Unread notifications persist across sessions until explicitly read.
- AC5: Real-time delivery via WebSocket; no page refresh required.

---

### 7. UI/UX Requirements

#### 7.1 General UI Standards

**User Story:** As a user, I want a consistent, modern interface so that the system is easy and pleasant to use.

**Acceptance Criteria:**
- AC1: The application uses a responsive layout that works on desktop (1280px+), tablet (768px–1279px), and mobile (320px–767px).
- AC2: All interactive elements have a minimum touch target of 44×44px.
- AC3: Color contrast ratios meet WCAG 2.1 AA standards.
- AC4: All data tables support column sorting; default sort is by date descending.
- AC5: Skeleton screens are shown for all async data loads; no blank white flashes.
- AC6: All destructive actions (delete, reject, deactivate) require a confirmation dialog.
- AC7: Form validation errors appear inline below the relevant field, not in a separate alert box.
- AC8: The application supports keyboard navigation throughout.

#### 7.2 Navigation & Layout

**Acceptance Criteria:**
- AC1: **[CHANGED v1.1]** The application uses an industry-standard shell layout:
  - **Top Header Bar** (fixed, full-width): App logo/name on the left; global search in the center; notification bell with unread badge, role-switcher (if multi-role), user avatar + name with dropdown (Profile, Settings, Logout) on the right.
  - **Left Sidebar** (collapsible): Role-driven navigation grouped into labeled sections. Collapses to an icon-only rail (48px wide) on toggle or on screens < 1024px. Sidebar state is persisted in localStorage.
  - **Main Content Area**: Fills remaining space; has a breadcrumb trail directly below the header for deep navigation paths.
  - **Contextual Action Bar**: A sticky bar at the bottom of forms/detail pages containing primary and secondary action buttons (e.g., Submit, Save Draft, Cancel).
- AC2: **[CHANGED v1.1]** Sidebar navigation structure per role:

  **Employee sidebar:**
  - 📊 Dashboard
  - ⏱ Log Time
  - 📋 My Timesheets (with sub-items: Weekly View, History)
  - 👤 My Profile

  **Manager sidebar (all Employee items plus):**
  - 👥 My Team (with sub-items: Team Dashboard, Review Timesheets, Team Reports)
  - 🔔 Send Reminder *(new)*

  **HR sidebar (all Employee items plus):**
  - 🏢 HR Overview
  - 🔔 Reminders
  - 🏖️ Holiday Calendar *(new)*
  - 📈 HR Reports

  **Admin sidebar (all sections plus):**
  - ⚙️ Administration (with sub-items: Users, Projects, Manager Assignments, Holiday Calendar, System Config, Audit Log)

- AC3: Active navigation item is highlighted with the amber accent color; parent group is expanded and highlighted when a child route is active.
- AC4: Page transitions use a subtle fade-slide animation (200ms).
- AC5: A persistent breadcrumb trail (e.g., "My Team > John Doe > Week of Apr 14") is shown below the header for all drill-down views, with each segment being a clickable link.

#### 7.3 Date & Time Handling

**Acceptance Criteria:**
- AC1: **[CHANGED v1.1]** All dates and times are stored in UTC in the database. The frontend detects the user's local timezone via the browser's `Intl.DateTimeFormat` API on login and stores it in the user session. All displayed dates and times are converted to the user's local timezone. The user's detected timezone is shown in their profile page and can be manually overridden.
- AC2: **[NEW v1.1]** When a user's local timezone differs from UTC, the "Log Time" page shows a timezone indicator (e.g., "Logging for: Monday, Apr 14 — IST (UTC+5:30)") so the user is always aware of which calendar date their entry will be recorded against.
- AC3: **[NEW v1.1]** The system uses the user's local timezone to determine: (a) which calendar date "today" is for logging purposes, (b) when EOD reminders are triggered (5 PM in the user's local timezone), and (c) which dates are flagged as missed.
- AC4: Date pickers default to today (in local timezone) and highlight the current date.
- AC5: Hours input uses a stepper control with 0.5-hour increments and also accepts direct keyboard input.
- AC6: Relative timestamps (e.g., "2 hours ago") are shown in activity feeds; absolute timestamps in local timezone are shown on hover.

#### 7.4 Empty States

**Acceptance Criteria:**
- AC1: Every list, table, and dashboard section has a designed empty state with an illustration, a brief message, and a relevant CTA (e.g., "No entries yet — Log your first entry").
- AC2: Empty states are distinct from error states; error states include a retry button.

#### 7.6 Weekend Work Handling **[NEW v1.1]**

**User Story:** As an employee who works on weekends, I want to log my weekend hours so that my extra effort is recorded and visible.

**Acceptance Criteria:**
- AC1: The weekly view (Mon–Sun) always shows Saturday and Sunday columns. They are visually distinct from weekdays — muted background, "Weekend" label, and an optional overtime icon.
- AC2: Employees can log task entries on Saturday and Sunday using the same task-level entry form as weekdays.
- AC3: Weekend entries follow the same 9-hour daily cap and the same PENDING → APPROVED/REJECTED workflow.
- AC4: Weekend days with no entries are never flagged as "Missed" and never trigger automated reminders.
- AC5: Weekend entries are included in weekly total hours on the dashboard and in all reports.
- AC6: The manager's team review view also shows Saturday and Sunday columns so weekend entries are visible for approval.
- AC7: HR reports include a separate "Weekend Hours" column to distinguish overtime from standard hours.
- AC8: The Admin can configure whether weekend logging is enabled or disabled org-wide (default: enabled).



**Acceptance Criteria:**
- AC1: API errors display a user-friendly message (not a raw error code) with a retry option.
- AC2: Network connectivity loss shows a persistent banner: "You're offline. Changes will sync when reconnected."
- AC3: Long-running operations (>500ms) show a progress indicator.
- AC4: Session expiry shows a modal prompting re-login without losing the current page state.

---

### 8. Non-Functional Requirements

#### 8.1 Performance

- NFR1: Page initial load time must be under 3 seconds on a standard broadband connection.
- NFR2: API response time for all read operations must be under 500ms at the 95th percentile under normal load.
- NFR3: API response time for write operations must be under 1 second at the 95th percentile.
- NFR4: The system must support at least 500 concurrent users without degradation.
- NFR5: Database queries must use appropriate indexes; full table scans on large tables are not permitted in production.

#### 8.2 Security

- NFR1: All data in transit must be encrypted using TLS 1.2 or higher.
- NFR2: Passwords must be stored as bcrypt hashes (cost factor ≥ 12).
- NFR3: All API endpoints must validate and sanitize inputs to prevent SQL injection and XSS attacks.
- NFR4: JWT tokens must be signed with RS256 and stored in HttpOnly cookies (not localStorage).
- NFR5: CORS policy must restrict origins to the configured frontend domain.
- NFR6: Sensitive fields (e.g., email, employee ID) must be masked in logs.
- NFR7: The application must pass OWASP Top 10 vulnerability checks before production deployment.

#### 8.3 Reliability & Availability

- NFR1: The system must achieve 99.5% uptime during business hours (Mon–Fri, 8 AM–8 PM local time).
- NFR2: Scheduled maintenance windows must be communicated 24 hours in advance via in-app banner.
- NFR3: Database backups must run daily with a 30-day retention period.
- NFR4: The system must recover from a single-node failure within 5 minutes.

#### 8.4 Scalability

- NFR1: The backend must be stateless to support horizontal scaling.
- NFR2: The database schema must support multi-tenancy (multiple organizations) as a future upgrade path without requiring a full redesign.
- NFR3: File exports (CSV, PDF) must be generated asynchronously for datasets exceeding 1,000 rows.

#### 8.5 Maintainability

- NFR1: Backend code must achieve at least 80% unit test coverage.
- NFR2: All API endpoints must be documented via OpenAPI 3.0 (Swagger).
- NFR3: Frontend components must follow Angular best practices including lazy loading for feature modules.
- NFR4: Database migrations must be managed via a versioned migration tool (e.g., Flyway).

#### 8.6 Accessibility

- NFR1: The application must meet WCAG 2.1 Level AA guidelines.
- NFR2: All images and icons must have descriptive alt text or aria-labels.
- NFR3: Screen reader compatibility must be verified for all primary user flows.

---

### 9. Data Requirements

#### 9.1 Data Retention

- DR1: Timesheet entries and audit logs must be retained for a minimum of 2 years.
- DR2: Deleted user accounts must have their personal data anonymized after 90 days (GDPR-aligned).
- DR3: Notification logs must be retained for 6 months.

#### 9.2 Data Integrity

- DR1: All foreign key relationships must be enforced at the database level.
- DR2: Timesheet entries must be immutable once APPROVED unless an Admin explicitly unlocks them; any unlock is logged.
- DR3: **[CHANGED v1.1]** A soft warning is triggered at 8 hours per day per employee. **[CHANGED v1.3]** Submissions exceeding 9 hours are permitted only when an overtime justification comment is provided; this is enforced at the API level. There is no hard database cap on daily hours.

#### 9.3 Core Data Entities

| Entity | Key Attributes |
|---|---|
| User | id, full_name, email, password_hash, roles, department, employee_id, timezone, status, created_at |
| Project | id, name, code, client, start_date, end_date, status, created_at |
| Timesheet_Entry | id, user_id, project_id, date (UTC), task_name, task_description, hours, status, overtime_justification, submitted_at, updated_at |
| Approval_Action | id, entry_id, actor_id, action, reason, created_at |
| Manager_Assignment | id, employee_id, manager_id, effective_from, effective_to |
| Clarification_Message | id, entry_id, author_id, message, created_at |
| Audit_Log | id, actor_id, action_type, entity_type, entity_id, before_value, after_value, created_at |
| Notification | id, user_id, type, message, is_read, deep_link, created_at |
| Reminder_Log | id, sent_by, sender_role, recipient_type, recipient_count, sent_at |
| System_Config | key, value, updated_by, updated_at |
| Holiday_Calendar | id, name, date (UTC), type, applicable_to, created_by, created_at |

---

### 10. Integration Requirements

#### 10.1 Email Service

- IR1: The system integrates with an SMTP-compatible email service (configurable host, port, credentials).
- IR2: Email templates are stored server-side and support variable substitution (e.g., `{{employee_name}}`, `{{entry_date}}`).
- IR3: A test-email feature is available in Admin settings to verify SMTP configuration.

#### 10.2 Future Integrations (Out of Scope for v1.0)

- IR1: LDAP/Active Directory for SSO authentication.
- IR2: Slack/Teams notifications as an alternative to email.
- IR3: Payroll system export (hours data in standard format).
- IR4: AI-based task categorization from free-text descriptions.
- IR5: Calendar integration (Google Calendar / Outlook) for auto-populating work days.

---

### 11. Constraints & Assumptions

- C1: The system is a single-tenant deployment for v1.0.
- C2: All users access the system via a modern web browser (Chrome 110+, Firefox 110+, Edge 110+, Safari 16+); no native mobile app is required for v1.0.
- C3: The work week is Monday–Friday; weekends are non-working days by default (configurable by Admin).
- C4: One employee maps to exactly one manager in v1.0; multi-manager support is a future enhancement.
- C5: Time is logged in hours with 0.5-hour precision; minute-level precision is not required for v1.0.
- C6: The system does not integrate with payroll or HR systems in v1.0.
- C7: All monetary values and billing rates are out of scope for v1.0.
- C8: The primary language of the UI is English; multi-language support is a future enhancement.
- A1: All users have a valid company email address.
- A2: The organization has a designated Admin user before go-live.
- A3: Projects are pre-loaded by the Admin before employees begin logging time.
- A4: The SMTP server credentials are provided by the organization's IT team.

---

### 12. Acceptance Testing Scenarios

#### Scenario 1 — Happy Path: Employee Logs Tasks and Gets Approved
1. Employee logs in → lands on role-specific dashboard (Employee view).
2. Employee navigates to "Log Time" → sees today's date with timezone indicator (e.g., "IST UTC+5:30").
3. Employee adds Task 1: Project A, "Backend API development", 4 hrs. Running total shows 4 hrs (under 8 hr warning threshold).
4. Employee adds Task 2: Project B, "Code review", 3 hrs. Running total shows 7 hrs.
5. Employee submits → both task entries saved as PENDING. Day_Status = PENDING.
6. Manager receives email notification.
7. Manager logs in → Manager Dashboard shows 1 pending day for the employee.
8. Manager drills into employee → expands the day → sees both tasks with individual statuses.
9. Manager approves both tasks at day level.
10. Both task statuses → APPROVED. Day_Status → APPROVED.
11. Employee receives approval email. Dashboard KPI cards update.

#### Scenario 2 — Partial Rejection: Mixed Day Status
1. Manager reviews a day with 3 task entries: Task A (PENDING), Task B (PENDING), Task C (PENDING).
2. Manager approves Task A and Task B individually.
3. Manager rejects Task C with reason "Wrong project assigned."
4. Task A and Task B → APPROVED. Task C → REJECTED.
5. Day_Status → REJECTED (rejection takes precedence).
6. Employee receives rejection email for Task C.
7. Employee edits Task C (corrects project), resubmits → Task C resets to PENDING.
8. Day_Status → CLARIFICATION_REQUESTED is not applicable here; Day_Status → PENDING (mixed: 2 APPROVED + 1 PENDING).
9. Manager approves Task C → Day_Status → APPROVED.

#### Scenario 3 — Clarification Flow with Day Status Impact
1. Manager clicks "Ask Clarification" on Task B of a day that has Task A (APPROVED) and Task B (PENDING).
2. Task B status → CLARIFICATION_REQUESTED. Day_Status → CLARIFICATION_REQUESTED.
3. Employee receives email, responds in the clarification thread.
4. Manager approves Task B from within the thread.
5. Day_Status → APPROVED (all tasks now APPROVED).

#### Scenario 4 — Missed Date Reminder (Weekday Only)
1. Employee does not log time on Monday (weekday).
2. At 5 PM in the employee's local timezone, the system detects the missed date.
3. Employee and manager receive automated reminder emails.
4. HR dashboard shows the employee in the "Missed Entries" count.
5. Manager sends an additional manual reminder from the Manager Dashboard.
6. Employee logs the missed entry (within the 30-day edit window).
7. Saturday and Sunday with no entries are NOT flagged as missed — no reminders sent.

#### Scenario 5 — Weekend Work Logging
1. Employee works on Saturday and navigates to the weekly view.
2. Saturday column is visible with a "Weekend" label and muted background.
3. Employee clicks the quick-add button on Saturday, logs 3 hours on a task.
4. Entry saved as PENDING. Saturday column shows 3 hrs with PENDING badge.
5. Manager sees the Saturday entry in the team review view and approves it.
6. HR report shows 3 hours under "Weekend Hours" for that employee.

#### Scenario 6 — Overtime Warning with Justification Comment
1. Employee adds tasks totalling 8 hours → soft warning banner appears: "You've reached the standard 8-hour day."
2. Employee adds another 1-hour task → total = 9 hours → warning escalates: "You're logging overtime. Please provide a justification before submitting."
3. Overtime justification field appears; employee enters "Client deadline — emergency release." Submit button becomes active.
4. Employee submits → entries saved as PENDING with overtime justification stored.
5. Manager opens team review → day column shows ⚠️ overtime badge → expands day → sees amber-highlighted tasks and justification comment inline.

#### Scenario 7 — Multi-Timezone Team
1. Employee A is in IST (UTC+5:30), Employee B is in EST (UTC-5).
2. Both log time on "Monday" — each sees Monday in their own local timezone.
3. Entries are stored in UTC; the manager (in a third timezone) sees each entry labeled with the employee's local date.
4. EOD reminders fire at 5 PM IST for Employee A and 5 PM EST for Employee B independently.

#### Scenario 8 — Admin Manages Users & Projects
1. Admin creates a new user with Employee role, assigns a manager, sets timezone to IST.
2. New user receives a welcome email with a temporary password.
3. New user logs in, is forced to change password.
4. Admin creates a new project and assigns the new user to it.
5. New user logs time against the new project successfully.

#### Scenario 9 — Holiday Exclusion from Missed Dates **[CHANGED v1.6]**
1. HR adds "Republic Day" as a Public Holiday on Jan 26.
2. Jan 26 falls on a Monday (a standard Work_Day).
3. Employee A does not log time on Jan 26 — no missed-date flag is set, no reminder is sent.
4. Employee B chooses to work and logs 4 hours on Jan 26 — entry is accepted, treated as overtime, saved as PENDING.
5. EOD reminder check runs — Jan 26 is identified as a holiday and skipped for all employees regardless of whether they logged time.
6. HR compliance dashboard shows Jan 26 as a holiday for all employees; Employee B's logged hours appear under overtime.
7. Employee A's weekly view shows Jan 26 with a 🏖️ icon and a "Log Time" button (no amber missed-date highlight).
8. Employee B's weekly view shows Jan 26 with a 🏖️ icon and a PENDING badge for the logged entry.

#### Scenario 10 — Auto-Approval for Sub-1-Hour Task **[NEW v1.5]**
1. Employee logs Task A: 4 hrs (PENDING) and Task B: 0.5 hrs (a quick admin task).
2. On submission, Task B (0.5 hrs) is immediately auto-approved by the system.
3. Task B shows status "Auto-Approved" in the employee's weekly view.
4. Task A remains PENDING awaiting manager review.
5. Day_Status = PENDING (Task A still pending).
6. Manager reviews and approves Task A → Day_Status → APPROVED.
7. Manager's review view shows Task B as "Auto-Approved" — no action required.

#### Scenario 11 — Auto-Approved Task Edited to Exceed 1 Hour **[EDGE CASE v1.5]**
1. Employee has Task B auto-approved at 0.5 hrs.
2. Employee realises the task took longer and edits Task B to 2 hrs.
3. System removes the "Auto-Approved" label; Task B status resets to PENDING.
4. Manager receives notification that a previously approved entry has been modified.
5. Manager reviews and approves Task B normally.

#### Scenario 12 — Retroactive Holiday Addition **[EDGE CASE v1.5]**
1. HR adds a company holiday retroactively for last Monday (which was already flagged as missed for 3 employees).
2. System automatically clears the missed-date flag for those 3 employees for that date.
3. Any pending reminders for that date are cancelled.
4. HR compliance dashboard updates to reflect the holiday — those employees are no longer shown as non-compliant.

#### Scenario 13 — Manager Reassignment Mid-Week **[EDGE CASE]**
1. Employee has 3 PENDING task entries submitted on Monday, assigned to Manager A.
2. Admin reassigns the employee to Manager B on Wednesday.
3. The 3 Monday entries remain with Manager A for approval (entries retain the manager at time of submission).
4. New entries submitted from Wednesday onwards are routed to Manager B.
5. Both Manager A and Manager B see only their respective entries in their review queues.

#### Scenario 14 — Manager Approving Their Own Timesheet **[EDGE CASE]**
1. Manager submits their own timesheet entries for the week.
2. The system detects that the actor is the same as the employee and blocks self-approval.
3. The entries are routed to the manager's own manager (skip-level) for approval.
4. If the manager has no manager assigned, entries are routed to any HR user for approval.
5. The manager's review queue does not show their own entries.

#### Scenario 15 — Employee Logs Time After Manager Reassignment on Same Day **[EDGE CASE]**
1. Employee submits Task A at 9 AM — routed to Manager A.
2. Admin reassigns employee to Manager B at 11 AM.
3. Employee submits Task B at 2 PM — routed to Manager B.
4. Manager A sees only Task A; Manager B sees only Task B.
5. Day_Status is computed across all tasks regardless of which manager owns them.

#### Scenario 16 — All Tasks on a Day Are Auto-Approved **[EDGE CASE v1.5]**
1. Employee logs 3 tasks, all at 0.5 hrs each (total 1.5 hrs).
2. All 3 tasks are auto-approved immediately on submission.
3. Day_Status → APPROVED immediately, with no manager action required.
4. Manager's review queue does not show this day.
5. Employee's weekly view shows the day as APPROVED with "Auto-Approved" labels on each task.

---

## Changelog

### Version 1.6 — Changes from v1.5

| # | Section | Change Type | Description |
|---|---|---|---|
| 1 | 4.4 Holiday Calendar — AC4 | **CHANGED** | Clarified: holidays never trigger missed-date flags or alerts regardless of whether the employee logged time. Logging on holidays is always permitted |
| 2 | 4.4 Holiday Calendar — AC6, AC7 | **CHANGED** | Holiday entries are treated as overtime and follow normal approval workflow; compliance exclusion applies regardless of whether time was logged |
| 3 | 2.4 Weekly View — AC4 | **CHANGED** | Holiday days show a "Log Time" button (voluntary) but no amber missed-date CTA |
| 4 | 2.7 Universal Read Access | **NEW** | New section: employees, managers, HR, and Admin can always view all entry data regardless of status. Locked entries are read-only but always visible. Clarification threads always readable. History view defaults to 3 months but supports full history |
| 5 | 12. Scenario 9 | **CHANGED** | Updated to show both an employee who skips the holiday and one who logs time — both handled correctly |

### Version 1.5 — Changes from v1.4

| # | Section | Change Type | Description |
|---|---|---|---|
| 1 | Glossary — Missed_Date | **CHANGED** | Organisation holidays now excluded from missed-date definition alongside weekends |
| 2 | Glossary — Organisation_Holiday | **NEW** | New term defined: company-wide non-working day configured by HR or Admin |
| 3 | 4.4 Holiday Calendar | **NEW** | Full section added: HR/Admin-only management, holiday types, department scope, weekly view integration, retroactive holiday edge cases |
| 4 | 7.2 Navigation — HR sidebar | **CHANGED** | Added 🏖️ Holiday Calendar to HR sidebar |
| 5 | 7.2 Navigation — Admin sidebar | **CHANGED** | Added Holiday Calendar to Admin Administration sub-items |
| 6 | 2.4 Weekly View — AC4 | **CHANGED** | Holiday dates shown with 🏖️ icon; no "Log Now" CTA on holidays |
| 7 | 2.1 Time Entry — AC5 | **CHANGED** | Tasks with hours < 1 (0.5 hrs) are auto-approved on submission; labelled "Auto-Approved" in UI and audit log |
| 8 | 3.3 Approval Actions — AC6, AC7 | **NEW** | Auto-approval rules for Day_Status computation; editing an auto-approved entry resets it to PENDING |
| 9 | 6.1 Notifications table | **CHANGED** | Missed-date event now explicitly excludes holidays |
| 10 | 9.3 Data Entities | **NEW** | Added Holiday_Calendar entity |
| 11 | 12. Scenarios 6–16 | **CHANGED / NEW** | Scenario 6 corrected (overtime warning flow); Scenarios 9–16 added covering holidays, auto-approval, edge cases for manager reassignment, self-approval routing, and mixed auto-approval days |

### Version 1.4 — Changes from v1.3

| # | Section | Change Type | Description |
|---|---|---|---|
| 1 | 2.1 Time Entry — AC9 | **CHANGED** | Overtime alert changed from in-app notification panel to task-level UI highlight. Manager sees an ⚠️ badge on the day column and an amber highlight on each contributing task with the justification comment shown inline. No notification panel entry, no email |
| 2 | 6.1 Notifications table | **CHANGED** | Overtime submission event updated to "task-level highlight in review UI only" |

### Version 1.3 — Changes from v1.2

| # | Section | Change Type | Description |
|---|---|---|---|
| 1 | 2.1 Time Entry — AC4 | **CHANGED** | 9-hour threshold is no longer a hard block. Exceeding 9 hours shows a warning and requires a mandatory overtime justification comment before submission. No hard cap enforced |
| 2 | 2.1 Time Entry — AC9 | **NEW** | Overtime submissions trigger an explicit in-app-only notification to the manager with the employee name, hours, date, and justification reason. No email sent for this event |
| 3 | 6.1 Notifications table | **NEW** | Added "Overtime submission" event — in-app only, no email |
| 4 | 9.2 Data Integrity — DR3 | **CHANGED** | Removed hard database cap; API enforces justification comment requirement when daily total > 9 hours |
| 5 | 9.3 Data Entities — Timesheet_Entry | **CHANGED** | Added `overtime_justification` field |
| 6 | 5.4 System Config — AC1 | **CHANGED** | "Hard cap" replaced with "overtime threshold requiring justification comment" |

### Version 1.2 — Changes from v1.1

| # | Section | Change Type | Description |
|---|---|---|---|
| 1 | 1.2 RBAC — AC3, AC4 | **CHANGED** | Unauthorized sections are now hidden entirely from the UI rather than returning HTTP 403. Direct URL access to a hidden route redirects to the user's dashboard. HTTP 403 is retained as a backend safety net only, never surfaced to the user |

### Version 1.1 — Changes from v1.0

| # | Section | Change Type | Description |
|---|---|---|---|
| 1 | Glossary — Timesheet_Entry | **CHANGED** | Entry is now task-level (includes Task Name field), not just a day-level hour block |
| 2 | Glossary — Work_Day / Work_Week | **CHANGED** | Work_Week now spans Mon–Sun; weekends are optional overtime days, not standard work days |
| 3 | Glossary — Day_Status | **NEW** | Added computed Day_Status derived from task-level statuses with defined precedence rules |
| 4 | 1.2 RBAC — AC3, AC6, AC7 | **CHANGED / NEW** | Role now drives the entire UI: sidebar sections, dashboard widgets, and action buttons are role-specific. Added role-UI mapping table and role-switcher for multi-role users |
| 5 | 2.1 Time Entry — AC2 | **CHANGED** | Entry now requires Task Name field; hours range changed from 0.5–24 to 0.5–9 |
| 6 | 2.1 Time Entry — AC4 | **CHANGED** | Daily warning threshold changed from 24 hrs to 8 hrs; hard submission block at 9 hrs |
| 7 | 2.1 Time Entry — section title | **CHANGED** | Renamed from "Daily Time Entry" to "Task-Level Time Entry" to reflect task-centric model |
| 8 | 2.4 Weekly Time View — AC1 | **CHANGED** | Weekly view now shows Mon–Sun (7 days); weekends shown with distinct "Weekend" label |
| 9 | 2.4 Weekly Time View — AC4 | **CHANGED** | Missed date logic now explicitly excludes weekends |
| 10 | 2.4 Weekly Time View — AC6, AC7 | **NEW** | Day_Status computation rules and mixed-status day indicator added |
| 11 | 3.2 Team Review — AC7 | **NEW** | Manager view handles mixed-status days; can still act on individual tasks |
| 12 | 4.2 Reminders — AC3, AC4 | **NEW** | Managers can now send reminders to their own direct reports |
| 13 | 4.2 Reminders — AC6 | **REMOVED** | Opt-out for automated reminders removed; all users receive reminders as configured |
| 14 | 6.1 Notifications — AC1 table | **CHANGED** | Added "Reminder sent by Manager" and "Weekend entry submitted" notification events |
| 15 | 7.2 Navigation & Layout | **CHANGED** | Full industry-standard shell layout defined: fixed top header, collapsible left sidebar, breadcrumb trail, contextual action bar. Role-specific sidebar structure documented |
| 16 | 7.3 Date & Time Handling | **CHANGED** | Explicit timezone handling: UTC storage, browser-detected local timezone for display, per-user timezone override, timezone indicator on Log Time page, EOD reminders fire in user's local timezone |
| 17 | 7.6 Weekend Work Handling | **NEW** | Full section added covering weekend logging UI, cap rules, approval workflow, reporting, and admin toggle |
| 18 | 9.2 Data Integrity — DR3 | **CHANGED** | Daily hours hard cap changed from 24 to 9; soft warning at 8 hours |
| 19 | 9.3 Data Entities — User | **CHANGED** | Added `timezone` field to User entity |
| 20 | 9.3 Data Entities — Timesheet_Entry | **CHANGED** | Added `task_name` field; `date` now explicitly noted as UTC |
| 21 | 9.3 Data Entities — Reminder_Log | **CHANGED** | Added `sender_role` field to distinguish HR vs Manager reminders |
| 22 | 5.4 System Config — AC1 | **CHANGED** | Daily hours cap split into warning threshold (8 hrs) and hard cap (9 hrs); weekend logging toggle added |
| 23 | 12. Acceptance Scenarios | **CHANGED** | All scenarios updated to reflect task-level logging, Day_Status rules, timezone handling, weekend work, and manager reminders. Added Scenarios 5–8 |

---

*End of Requirements Document*

