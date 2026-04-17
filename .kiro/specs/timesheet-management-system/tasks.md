# Implementation Tasks — Timesheet Management System (TMS)

## Phase 1: Project Setup & Infrastructure

- [x] 1. Initialize Spring Boot backend project
  - [x] 1.1 Create Spring Boot 3.x project with Java 21 using Spring Initializr (Web, Security, JPA, Mail, WebSocket, Validation, Flyway, Actuator)
  - [x] 1.2 Configure MS SQL Server datasource pointing to TMS database with TMS4 schema as default
  - [x] 1.3 Configure Flyway for versioned migrations under `db/migration/`
  - [x] 1.4 Set up OpenAPI 3.0 (Springdoc) with Swagger UI at `/swagger-ui.html`
  - [x] 1.5 Configure Spring Security skeleton (permit `/api/auth/**`, secure all others)
  - [x] 1.6 Configure `ThreadPoolTaskExecutor` for async report generation
  - [x] 1.7 Configure CORS to allow Angular dev origin; restrict to frontend domain in prod

- [ ] 2. Initialize Angular 21 frontend project
  - [ ] 2.1 Scaffold Angular 21 project: `ng new tms-frontend --style=scss --strict`
  - [ ] 2.2 Install Angular Material 21, NgRx, and configure global SCSS with TNS design tokens
  - [ ] 2.3 Set up app shell structure: `core/`, `shared/`, `features/` directories
  - [ ] 2.4 Configure lazy-loaded root routes in `app.routes.ts`
  - [ ] 2.5 Set up NgRx store with `auth`, `notifications`, `timesheet`, `config` slices

- [x] 2.8 Write unit tests for `TmsApplication` context load — `@SpringBootTest` smoke test confirming application context starts
- [ ] 2.9 Run `mvn clean install` — Phase 1 build must pass with zero errors before proceeding

---

## Phase 2: Database Schema (TMS4 Schema)

- [x] 3. Create database migration scripts for all core tables
  - [x] 3.1 `V1__create_users.sql` — `TMS4.users` table
  - [x] 3.2 `V2__create_user_roles.sql` — `TMS4.user_roles` join table
  - [x] 3.3 `V3__create_password_reset_tokens.sql` — `TMS4.password_reset_tokens`
  - [x] 3.4 `V4__create_projects.sql` — `TMS4.projects`
  - [x] 3.5 `V5__create_project_assignments.sql` — `TMS4.project_assignments`
  - [x] 3.6 `V6__create_manager_assignments.sql` — `TMS4.manager_assignments`
  - [x] 3.7 `V7__create_timesheet_entries.sql` — `TMS4.timesheet_entries`
  - [x] 3.8 `V8__create_approval_actions.sql` — `TMS4.approval_actions`
  - [x] 3.9 `V9__create_clarification_messages.sql` — `TMS4.clarification_messages`
  - [x] 3.10 `V10__create_audit_logs.sql` — `TMS4.audit_logs`
  - [x] 3.11 `V11__create_notifications.sql` — `TMS4.notifications`
  - [x] 3.12 `V12__create_reminder_logs.sql` — `TMS4.reminder_logs`
  - [x] 3.13 `V13__create_holiday_calendar.sql` — `TMS4.holiday_calendar`
  - [x] 3.14 `V14__create_system_config.sql` — `TMS4.system_config` with default seed values
  - [x] 3.15 `V15__create_export_jobs.sql` — `TMS4.export_jobs`
  - [x] 3.16 `V16__create_indexes.sql` — indexes on foreign keys and frequently queried columns
  - [ ] 3.17 Write Flyway migration integration test — `@SpringBootTest` verifying all migrations run cleanly against test schema
  - [ ] 3.18 Run `mvn clean install` — Phase 2 build must pass (all migrations apply, context loads)
  - [x] **All 15 tables created directly in TMS.TMS4 schema via MCP SQL Server connection**

---

## Phase 3: Backend — Authentication & Authorization

- [x] 4. Implement JWT authentication
  - [x] 4.1 Create `JwtService` — HS256 token generation and validation, jti claim for blacklisting
  - [x] 4.2 Create `TokenBlacklist` — in-memory blacklist for logout invalidation
  - [x] 4.3 Create `JwtAuthFilter` — extracts JWT from HttpOnly cookie, validates, sets `SecurityContext`
  - [x] 4.4 Configure `SecurityConfig` — filter chain, CSRF, CORS, session stateless
  - [x] 4.5 Implement `AuthService.authenticate()` — bcrypt verify, failed attempt tracking, account lock at 5 attempts, 15-min lock, audit log
  - [x] 4.6 Implement `AuthService.logout()` — blacklist jti, clear cookie, audit log
  - [x] 4.7 Implement `AuthService.refresh()` — validate existing token, issue new token, set cookie
  - [x] 4.8 Create `AuthController` — POST `/api/auth/login`, `/logout`, `/refresh`

- [x] 5. Implement password management
  - [x] 5.1 Implement `PasswordResetService` — generate secure token, store bcrypt hash with 1hr expiry, single-use enforcement
  - [x] 5.2 Implement forgot-password flow — POST `/api/auth/forgot-password` always returns 200
  - [x] 5.3 Implement reset-password flow — POST `/api/auth/reset-password`
  - [x] 5.4 Implement change-password — POST `/api/auth/change-password`, enforce complexity
  - [ ] 5.5 Enforce first-login forced password change via `ForcePasswordChangeFilter`

- [x] 6. Implement RBAC
  - [x] 6.1 Create `Role` enum: `EMPLOYEE`, `MANAGER`, `HR`, `ADMIN`
  - [x] 6.2 Implement `CustomUserDetailsService` loading user + roles from DB
  - [x] 6.3 Add `@PreAuthorize` annotations on service methods with role checks
  - [ ] 6.4 Implement role-switcher endpoint — POST `/api/auth/switch-role`
  - [x] 6.5 Write unit tests for `AuthService` — 7 tests passing (valid credentials, wrong password, failed attempts, account lock, logout blacklist)
  - [x] 6.6 Write unit tests for `JwtService` — 6 tests passing (generation, validation, expiry, blacklist)
  - [x] 6.7 Write unit tests for `PasswordResetService` — 6 tests passing (token generation, single-use, expiry)
  - [ ] 6.8 Write integration test for login flow — POST `/api/auth/login` full request/response cycle
  - [ ] 6.9 Run `mvn clean install` — Phase 3 build must pass

---

## Phase 4: Backend — Timesheet Entry

- [x] 7. Implement timesheet entry CRUD
  - [x] 7.1 Create `TimesheetEntry` JPA entity mapped to `TMS4.timesheet_entries`
  - [x] 7.2 Create `TimesheetEntryRepository` with custom queries for weekly/daily fetch
  - [x] 7.3 Implement `TimesheetService.submitEntries()` — validate mandatory fields, enforce 30-day past limit, capture `manager_id_at_submission`, save as PENDING
  - [x] 7.4 Implement auto-approval logic — if `hours < 1` (0.5 hr), set `AUTO_APPROVED`, skip manager notification, audit log
  - [x] 7.5 Implement overtime validation — soft warning at 8 hrs, require `overtime_justification` when daily total > 9 hrs
  - [x] 7.6 Implement `TimesheetService.editEntry()` — only PENDING/CLARIFICATION_REQUESTED editable, reset to PENDING, notify manager, audit log
  - [x] 7.7 Implement `TimesheetService.deleteEntry()` — only PENDING deletable, audit log
  - [x] 7.8 Create `TimesheetController` — POST/PUT/DELETE `/api/timesheets/entries`

- [x] 8. Implement weekly view and dashboard data
  - [x] 8.1 Implement `DayStatusComputer` — stateless utility computing Day_Status from task statuses
  - [x] 8.2 Implement `TimesheetService.getWeeklySummary()` — fetch entries for week, flag missed dates
  - [ ] 8.3 Implement `TimesheetService.getEmployeeDashboard()` — KPIs endpoint
  - [x] 8.4 Implement paginated history endpoint with filters — GET `/api/timesheets/history`
  - [ ] 8.5 Implement CSV export for filtered history — GET `/api/timesheets/export`
  - [x] 8.6 Write unit tests for `TimesheetService` — 8 tests passing (auto-approve, overtime, future date, 30-day limit, edit/delete validation)
  - [x] 8.7 Write unit tests for `DayStatusComputer` — 11 tests + 2 jqwik property-based tests passing
  - [x] 8.8 Write unit tests for `OvertimeValidator` — 22 parameterized tests passing (boundary values at 8.0, 8.5, 9.0, 9.5)
  - [ ] 8.9 Write integration test for timesheet submission
  - [ ] 8.10 Run `mvn clean install` — Phase 4 build must pass

---

## Phase 5: Backend — Manager Approval & Clarification

- [x] 9. Implement approval workflow
  - [x] 9.1 Create `ApprovalAction` JPA entity and repository
  - [x] 9.2 Implement `ApprovalService.approveEntry()` — set APPROVED, notify employee, audit log, block self-approval
  - [x] 9.3 Implement `ApprovalService.rejectEntry()` — require reason (min 10 chars), set REJECTED, notify employee, audit log
  - [x] 9.4 Implement bulk approve/reject — day-level
  - [x] 9.5 Implement `ApprovalService.requestClarification()` — set CLARIFICATION_REQUESTED, notify employee
  - [x] 9.6 Create `ApprovalController` — all `/api/approvals/**` endpoints

- [x] 10. Implement clarification thread
  - [x] 10.1 Create `ClarificationMessage` JPA entity and repository
  - [x] 10.2 Implement `ClarificationService.postMessage()` — append message, notify other party
  - [x] 10.3 Enforce thread read-only once entry is APPROVED or REJECTED
  - [x] 10.4 Create `ClarificationController` — GET and POST `/api/clarifications/entries/{entryId}`

- [x] 11. Implement manager dashboard and team review
  - [x] 11.1 Implement `ManagerService.getDashboard()` — KPIs: total direct reports, pending approvals, approved this week
  - [x] 11.2 Implement `ManagerService.getTeamSummary()` — list direct reports with hours, pending count
  - [x] 11.3 Implement `ManagerService.getEmployeeWeeklyView()` — employee's weekly entries
  - [x] 11.4 Create `ManagerController` — all `/api/manager/**` endpoints
  - [x] 11.5 Write unit tests for `ApprovalService` — 7 tests passing (self-approval, no reason, valid rejection, clarification)
  - [x] 11.6 Write unit tests for `ClarificationService` — 5 tests passing (closed thread, open thread, notifications)
  - [ ] 11.7 Write unit tests for `ManagerService` — dashboard KPI calculations
  - [ ] 11.8 Write integration test for approval workflow
  - [ ] 11.9 Run `mvn clean install` — Phase 5 build must pass

---

## Phase 6: Backend — HR Features

- [x] 12. Implement HR dashboard and reports
  - [x] 12.1 Implement `HrService.getDashboard()` — org-wide KPIs: total employees, compliance rate, avg hours
  - [x] 12.2 Implement `HrService.getEmployeeDailySummary()` — aggregated hours per day (no task-level detail)
  - [x] 12.3 Implement async report generation — `ReportService.generateReport()` with `@Async`, creates `ExportJob`
  - [x] 12.4 Implement report types: Weekly Compliance, Monthly Hours Summary, etc.
  - [x] 12.5 Create `ReportController` — POST `/api/hr/reports/generate`, GET status endpoint

- [x] 13. Implement holiday calendar
  - [x] 13.1 Create `HolidayCalendar` JPA entity and repository
  - [x] 13.2 Implement CRUD for holidays — HR/Admin only for write, all roles for read
  - [ ] 13.3 Implement bulk CSV import for holidays
  - [x] 13.4 Implement retroactive holiday logic — when holiday added for past missed date
  - [x] 13.5 Integrate holiday check into missed-date detection and compliance calculations
  - [x] 13.6 Create `HolidayCalendarController` — all `/api/hr/holidays/**` endpoints

- [x] 14. Implement reminders
  - [x] 14.1 Implement `ReminderService.sendMissingEntryReminder()` — HR org-wide and Manager to direct reports
  - [x] 14.2 Implement `ReminderService.sendPendingApprovalReminder()` — HR sends to managers with stale items
  - [x] 14.3 Implement `ReminderService.sendEmployeeReminder()` — Manager sends to specific employee
  - [x] 14.4 Log all reminder sends to `TMS4.reminder_logs`
  - [ ] 14.5 Create `ReminderController` — all reminder endpoints for HR and Manager
  - [x] 14.6 Write unit tests for `HrService` — 3 tests passing (compliance rate, daily summary, no task detail)
  - [x] 14.7 Write unit tests for `HolidayCalendarService` — 6 tests passing (add, duplicate, retroactive, delete)
  - [ ] 14.8 Write unit tests for `ReminderService` — missing entry detection, pending approval detection
  - [x] 14.9 Write unit tests for `ReportService` — 3 tests passing (job creation, status transitions, async completion)
  - [ ] 14.10 Run `mvn clean install` — Phase 6 build must pass

---

## Phase 7: Backend — Admin Features

- [x] 15. Implement user management
  - [x] 15.1 Implement `UserService.createUser()` — create with roles, manager assignment, send welcome email
  - [x] 15.2 Implement `UserService.updateUser()`, `deactivateUser()`, `reactivateUser()`
  - [x] 15.3 Implement `UserService.resetPassword()` — trigger password reset email
  - [ ] 15.4 Implement bulk user CSV import with row-by-row error reporting
  - [x] 15.5 Create `UserController` — all `/api/admin/users/**` endpoints

- [x] 16. Implement project management
  - [x] 16.1 Implement `ProjectService` — CRUD, archive/restore, employee assignment
  - [x] 16.2 Ensure archived projects excluded from employee dropdown but history preserved
  - [ ] 16.3 Create `ProjectController` — all `/api/admin/projects/**` endpoints

- [x] 17. Implement manager assignments and system config
  - [x] 17.1 Implement `ManagerAssignmentService` — create/update assignment, prevent circular assignments
  - [x] 17.2 Implement org chart endpoint — `ManagerAssignmentController` with GET `/api/admin/manager-assignments`
  - [x] 17.3 Implement `SystemConfigService` — get/update config values, audit log on change, immediate effect
  - [x] 17.4 Create controllers for manager assignments and system config

- [x] 18. Implement audit log viewer
  - [x] 18.1 Implement `AuditLogService.log()` — called by all state-changing services
  - [x] 18.2 Implement audit log query with filters (date range, actor, action type, entity type)
  - [ ] 18.3 Implement CSV export for audit log
  - [x] 18.4 Create `AuditLogController` — GET `/api/admin/audit-log`
  - [x] 18.5 Write unit tests for `UserService` — 5 tests passing (create, duplicate email, deactivate, reactivate)
  - [x] 18.6 Write unit tests for `ProjectService` — 5 tests passing (create, duplicate name, archive, restore)
  - [x] 18.7 Write unit tests for `ManagerAssignmentService` — 4 tests passing (valid assign, circular dependency, self-assign, reassign)
  - [x] 18.8 Write unit tests for `SystemConfigService` — 4 tests passing (update, not found, get all, immediate effect)
  - [ ] 18.9 Write unit tests for `AuditLogService` — immutability tests
  - [ ] 18.10 Run `mvn clean install` — Phase 7 build must pass

---

## Phase 8: Backend — Notifications

- [x] 19. Implement email notifications
  - [x] 19.1 Configure `JavaMailSender` with SMTP settings
  - [ ] 19.2 Create server-side email templates (Thymeleaf)
  - [x] 19.3 Implement `NotificationService.sendEmail()` with retry logic (3 attempts, exponential backoff via Spring Retry)
  - [x] 19.4 Implement notification triggers (account locked, welcome email, password reset)
  - [ ] 19.5 Implement test-email endpoint — POST `/api/admin/email/test`

- [x] 20. Implement in-app WebSocket notifications
  - [x] 20.1 Configure Spring WebSocket with STOMP — `WebSocketConfig.java`
  - [x] 20.2 Implement `WebSocketNotificationSender` — push to `/user/queue/notifications` per user
  - [x] 20.3 Persist notifications to `TMS4.notifications` table
  - [x] 20.4 Create `NotificationController` — GET last 20, mark read, mark all read, unread count
  - [ ] 20.5 Write unit tests for `NotificationService` — retry logic tests
  - [ ] 20.6 Write unit tests for `WebSocketNotificationSender`
  - [ ] 20.7 Run `mvn clean install` — Phase 8 build must pass

---

## Phase 9: Backend — Scheduler

- [ ] 21. Implement scheduled jobs
  - [ ] 21.1 Implement `MissedDateDetectionJob` — runs daily at 5 PM per user timezone
  - [ ] 21.2 Implement `PendingApprovalReminderJob` — runs daily, finds pending entries > 2 business days
  - [ ] 21.3 Make scheduler times configurable via `system_config`
  - [ ] 21.4 Add `TMS4.scheduler_locks` table usage to prevent duplicate runs
  - [ ] 21.5 Write unit tests for `MissedDateDetectionJob`
  - [ ] 21.6 Write unit tests for `PendingApprovalReminderJob`
  - [ ] 21.7 Run `mvn clean install` — Phase 9 full backend build must pass

---

## Phase 10: Frontend — App Shell & Core

- [ ] 22. Implement core services and interceptors
  - [ ] 22.1 Implement `AuthService` — login, logout, refresh, role-switch; store user in NgRx auth slice
  - [ ] 22.2 Implement `JwtInterceptor` (functional) — attach credentials cookie, intercept 401 to trigger silent refresh
  - [ ] 22.3 Implement `AuthGuard` and `RoleGuard` — protect routes
  - [ ] 22.4 Implement `TimezoneService` — detect browser timezone via `Intl.DateTimeFormat`
  - [ ] 22.5 Implement `NotificationService` — STOMP WebSocket connection
  - [ ] 22.6 Implement `LocalDatePipe` and `RelativeTimePipe`

- [ ] 23. Implement app shell layout
  - [ ] 23.1 Create `ShellComponent` — fixed top header bar
  - [ ] 23.2 Create collapsible left sidebar — role-driven nav, collapses to 48px icon rail
  - [ ] 23.3 Implement role-switcher
  - [ ] 23.4 Create breadcrumb component
  - [ ] 23.5 Create skeleton screen components
  - [ ] 23.6 Create toast, confirmation dialog, empty state components
  - [ ] 23.7 Write Vitest unit tests for core services and pipes
  - [ ] 23.8 Run `ng build` — Phase 10 frontend build must compile with zero errors

---

## Phase 11: Frontend — Auth Pages

- [ ] 24. Implement auth feature (lazy-loaded, full-page layout, no shell)
  - [ ] 24.1 Create `LoginComponent`
  - [ ] 24.2 Create `ForgotPasswordComponent`
  - [ ] 24.3 Create `ResetPasswordComponent`
  - [ ] 24.4 Create `ChangePasswordComponent`
  - [ ] 24.5 Wire up `auth.routes.ts`
  - [ ] 24.6 Write Vitest unit tests for `LoginComponent`
  - [ ] 24.7 Write Vitest unit tests for `ResetPasswordComponent`
  - [ ] 24.8 Run `ng build` — Phase 11 build must pass

---

## Phase 12: Frontend — Employee Features

- [ ] 25. Implement employee dashboard
- [ ] 26. Implement log time page
- [ ] 27. Implement weekly view
- [ ] 28. Implement timesheet history

---

## Phase 13: Frontend — Manager Features

- [ ] 29. Implement manager dashboard
- [ ] 30. Implement team review
- [ ] 31. Implement clarification thread panel
- [ ] 32. Implement manager reports and reminders

---

## Phase 14: Frontend — HR Features

- [ ] 33. Implement HR dashboard
- [ ] 34. Implement holiday calendar management
- [ ] 35. Implement HR reminders and reports

---

## Phase 15: Frontend — Admin Features

- [ ] 36. Implement user management
- [ ] 37. Implement project management
- [x] 38. Implement manager assignments and org chart
  - [x] 38.1 Create `ManagerAssignmentsComponent` — list assignments, reassign manager, circular assignment prevention
  - [x] 38.2 Create `OrgChartComponent` — visual tree of manager-employee relationships
  - [x] `ManagerAssignmentController` — GET/POST `/api/admin/manager-assignments`
  - [x] Routes: `/admin/org-chart`, `/admin/manager-assignments`
- [ ] 39. Implement system config and audit log

---

## Phase 16: Non-Functional & Cross-Cutting

- [ ] 40. Implement error handling and resilience
  - [x] 40.1 Create `GlobalExceptionHandler` — maps exceptions to user-friendly API error responses
  - [ ] 40.2 Angular HTTP error interceptor
  - [ ] 40.3 Session expiry modal
  - [ ] 40.4 Long-running operations progress indicator

- [ ] 41. Implement security hardening
  - [x] 41.1 Jakarta Bean Validation on backend inputs
  - [ ] 41.2 Mask sensitive fields in logs
  - [x] 41.3 JWT stored in HttpOnly, Secure, SameSite=Strict cookie
  - [ ] 41.4 Configure TLS 1.2+
  - [x] 41.5 CORS restricted to configured frontend domain

- [ ] 42. Implement accessibility and UI polish (frontend — not started)

- [ ] 43. Testing
  - [x] 43.1 Backend unit tests (JUnit 5 + Mockito) — **105 tests passing** across all service classes
  - [ ] 43.2 Backend integration tests (Spring Boot Test) for key flows
  - [x] 43.3 Property-based tests (jqwik) for `DayStatusComputer` and `OvertimeValidator`
  - [ ] 43.4 Frontend unit tests (Vitest) — not started
  - [ ] 43.5 Property-based tests (fast-check) for Day_Status in Angular — not started
  - [ ] 43.6 Cypress E2E tests — not started
  - [ ] 43.7 Run final `mvn clean install` — all 80%+ coverage, zero failures
  - [ ] 43.8 Run final `ng build --configuration production` — not started
