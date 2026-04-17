# Design Document: Timesheet Management System (TMS)

## Overview

The Timesheet Management System (TMS) is a full-stack web application built for Think N Solutions. It enables employees to log daily work hours at the task level against projects, managers to review and approve those entries, HR to monitor compliance and generate reports, and Admins to configure the system. The architecture follows a clean separation between a Spring Boot REST API backend and an Angular 21 SPA frontend, backed by MS SQL Server.
Design Document
Timesheet Management System (TMS)
Version: 1.0
Date: 2025
Organization: Think N Solutions
Tech Stack: Java 21 · Spring Boot (latest) · Angular 21 · MS SQL Server
Requirements Version: 1.6

Overview
The Timesheet Management System (TMS) is a web-based application enabling employees to log daily work hours at the task level, managers to review and approve entries, and HR/Admin roles to oversee compliance, reporting, and system configuration. The system is built as a single-page Angular 21 frontend communicating with a Spring Boot REST API backed by MS SQL Server.

Key design principles: - Task-level granularity: Time is logged per task, not per day. Day-level status is computed from task statuses. - Role elevation model: Every user is an Employee; Manager/HR/Admin roles add capabilities on top. - UTC-first storage: All timestamps stored in UTC; timezone conversion happens at the API boundary and in the frontend. - Stateless backend: JWT-based authentication with HttpOnly cookies enables horizontal scaling. - Auto-approval: Tasks with hours < 1 (0.5 hr) are approved immediately on submission without manager action. - Computed Day_Status: Never stored — always derived from task-level statuses using defined precedence rules.

Architecture
System Architecture Diagram
graph TB
    subgraph Client["Browser (Angular 21 SPA)"]
        UI[Angular Components]
        Store[NgRx State Store]
        WS_Client[WebSocket Client]
    end

    subgraph Server["Spring Boot Application Server"]
        API[REST Controllers]
        Security[Spring Security / JWT Filter]
        Services[Service Layer]
        Scheduler[Scheduled Tasks]
        WS_Server[WebSocket Handler]
        AsyncExec[Async Executor]
    end

    subgraph Data["Data Layer"]
        DB[(MS SQL Server)]
        Flyway[Flyway Migrations]
    end

    subgraph Email["Email (Spring JavaMailSender)"]
        SMTP[Company Mail Server\nOffice 365 / Google Workspace / Exchange]
    end

    UI -->|HTTPS REST + HttpOnly JWT Cookie| API
    UI -->|WSS WebSocket| WS_Server
    API --> Security
    Security --> Services
    Services --> DB
    Scheduler --> Services
    WS_Server --> Services
    AsyncExec --> Services
    Services --> SMTP
    Flyway --> DB
Component Overview
Layer	Technology	Responsibility
Frontend SPA	Angular 21	UI rendering, state management, WebSocket client, timezone conversion
REST API	Spring Boot 3.x / Java 21	Business logic, RBAC enforcement, JWT issuance, validation
WebSocket	Spring WebSocket (STOMP)	Real-time in-app notification delivery
Database	MS SQL Server	Persistent storage, referential integrity, indexing
Email	Spring JavaMailSender (spring-boot-starter-mail)	Transactional email delivery via company mail server (Office 365 / Google Workspace / Exchange). Spring acts as the SMTP client — no separate email service or library required.
Migrations	Flyway	Versioned schema management
Scheduler	Spring @Scheduled	EOD missed-date detection, automated reminders
Async Executor	Spring @Async + ThreadPoolTaskExecutor	Report/export generation for large datasets
Communication Patterns
Frontend → Backend: HTTPS REST calls with JWT stored in HttpOnly, Secure, SameSite=Strict cookies. CSRF protection via double-submit cookie pattern.
Real-time notifications: STOMP over WebSocket (WSS). Client subscribes to /user/queue/notifications after authentication.
Async exports: Client polls a /api/exports/{jobId}/status endpoint or receives a WebSocket push when the export is ready.
Email: Spring Boot's JavaMailSender (from spring-boot-starter-mail) sends transactional emails by connecting to your company's existing mail server (Office 365, Google Workspace, or Exchange) over SMTP with STARTTLS. Spring is the SMTP client — no external email service or additional library is needed. Sending is synchronous with exponential-backoff retry (up to 3 attempts) on failure.
Components and Interfaces
Backend Package Structure
com.tns.tms
├── config/
│   ├── SecurityConfig.java          # Spring Security, JWT, CORS, CSRF
│   ├── WebSocketConfig.java         # STOMP WebSocket configuration
│   ├── AsyncConfig.java             # ThreadPoolTaskExecutor for async jobs
│   ├── FlywayConfig.java            # Flyway migration configuration
│   └── OpenApiConfig.java           # Swagger / OpenAPI 3.0
├── domain/
│   ├── user/
│   │   ├── User.java                # JPA entity
│   │   ├── Role.java                # Enum: EMPLOYEE, MANAGER, HR, ADMIN
│   │   ├── UserRepository.java
│   │   ├── UserService.java
│   │   └── UserController.java
│   ├── auth/
│   │   ├── AuthController.java      # /api/auth/**
│   │   ├── AuthService.java
│   │   ├── JwtService.java          # RS256 token issuance/validation
│   │   ├── TokenBlacklist.java      # Server-side JWT invalidation on logout
│   │   └── PasswordResetService.java
│   ├── timesheet/
│   │   ├── TimesheetEntry.java      # JPA entity
│   │   ├── TimesheetEntryRepository.java
│   │   ├── TimesheetService.java    # Core business logic
│   │   ├── TimesheetController.java # /api/timesheets/**
│   │   ├── DayStatusComputer.java   # Stateless utility: computes Day_Status
│   │   └── OvertimeValidator.java   # Validates 8hr warning / 9hr justification
│   ├── approval/
│   │   ├── ApprovalAction.java
│   │   ├── ApprovalActionRepository.java
│   │   ├── ApprovalService.java     # Approve/reject/clarification logic
│   │   └── ApprovalController.java  # /api/approvals/**
│   ├── clarification/
│   │   ├── ClarificationMessage.java
│   │   ├── ClarificationRepository.java
│   │   ├── ClarificationService.java
│   │   └── ClarificationController.java
│   ├── manager/
│   │   ├── ManagerAssignment.java
│   │   ├── ManagerAssignmentRepository.java
│   │   ├── ManagerAssignmentService.java
│   │   └── ManagerController.java   # /api/manager/**
│   ├── holiday/
│   │   ├── HolidayCalendar.java
│   │   ├── HolidayCalendarRepository.java
│   │   ├── HolidayCalendarService.java
│   │   └── HolidayCalendarController.java
│   ├── notification/
│   │   ├── Notification.java
│   │   ├── NotificationRepository.java
│   │   ├── NotificationService.java  # In-app + email dispatch
│   │   ├── NotificationController.java
│   │   └── WebSocketNotificationSender.java
│   ├── reminder/
│   │   ├── ReminderLog.java
│   │   ├── ReminderLogRepository.java
│   │   ├── ReminderService.java
│   │   └── ReminderController.java
│   ├── report/
│   │   ├── ReportService.java        # Async report generation
│   │   ├── ExportJob.java            # Tracks async export status
│   │   ├── ExportJobRepository.java
│   │   └── ReportController.java
│   ├── admin/
│   │   ├── ProjectController.java    # /api/admin/projects/**
│   │   ├── SystemConfigController.java
│   │   └── AuditLogController.java
│   └── audit/
│       ├── AuditLog.java
│       ├── AuditLogRepository.java
│       └── AuditLogService.java      # Called by all state-changing services
├── scheduler/
│   ├── MissedDateDetectionJob.java   # Runs at 5 PM per user timezone
│   └── PendingApprovalReminderJob.java
├── shared/
│   ├── dto/                          # Request/Response DTOs
│   ├── exception/                    # GlobalExceptionHandler, custom exceptions
│   ├── validation/                   # Custom JSR-380 validators
│   └── util/
│       ├── TimezoneUtil.java         # UTC <-> local timezone conversions
│       └── DateUtil.java
└── TmsApplication.java
Key REST API Endpoints
Authentication (/api/auth)
Method	Path	Description	Roles
POST	/api/auth/login	Authenticate, issue JWT cookie	Public
POST	/api/auth/logout	Invalidate JWT, clear cookie	Authenticated
POST	/api/auth/refresh	Silent token refresh	Authenticated
POST	/api/auth/forgot-password	Send password reset email	Public
POST	/api/auth/reset-password	Consume reset token, set new password	Public
POST	/api/auth/change-password	Change own password	Authenticated
Auth Request / Response DTOs
POST /api/auth/login

// Request
{
  "email": "john.doe@company.com",
  "password": "SecurePass1!"
}

// Response 200 OK — JWT set as HttpOnly cookie; body carries user context
{
  "userId": 42,
  "fullName": "John Doe",
  "email": "john.doe@company.com",
  "roles": ["EMPLOYEE", "MANAGER"],
  "activeRole": "MANAGER",
  "timezone": "Asia/Kolkata",
  "forcePasswordChange": false
}

// Response 401 Unauthorized — generic message (never reveals which field is wrong)
{ "error": "Invalid credentials" }

// Response 423 Locked
{ "error": "Account locked", "lockedUntil": "2025-04-14T10:15:00Z" }
POST /api/auth/forgot-password

// Request
{ "email": "john.doe@company.com" }

// Response 200 OK — always returns 200 even if email not found (prevents user enumeration)
{ "message": "If that email exists, a reset link has been sent." }
POST /api/auth/reset-password

// Request
{ "token": "<reset-token-from-email>", "newPassword": "NewSecure1!" }

// Response 200 OK
{ "message": "Password reset successfully. Please log in." }

// Response 400 — token expired or already used
{ "error": "Reset token is invalid or has expired." }
POST /api/auth/change-password (first-login forced change + voluntary change)

// Request
{ "currentPassword": "TempPass1!", "newPassword": "MyNewPass1!" }

// Response 200 OK
{ "message": "Password changed successfully." }
Auth Backend — Key Implementation Details
AuthController.java — thin controller, delegates all logic to AuthService:

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        LoginResponse loginResponse = authService.authenticate(request);
        // JWT set as HttpOnly, Secure, SameSite=Strict cookie by AuthService
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue("tms_jwt") String token,
            HttpServletResponse response) {
        authService.logout(token, response); // blacklists jti, clears cookie
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = "tms_jwt", required = false) String token,
            HttpServletResponse response) {
        authService.refresh(token, response);
        return ResponseEntity.noContent().build();
    }
}
AuthService.java — core authentication logic:

public LoginResponse authenticate(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    // Check account lock
    if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
        throw new AccountLockedException(user.getLockedUntil());
    }

    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        handleFailedAttempt(user); // increments counter, locks at 5
        throw new BadCredentialsException("Invalid credentials");
    }

    // Reset failed attempts on success
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userRepository.save(user);

    // Issue JWT
    String jwt = jwtService.generateToken(user);
    cookieService.setJwtCookie(response, jwt); // HttpOnly, Secure, SameSite=Strict

    auditLogService.log(user.getId(), "LOGIN", "USER", user.getId(), null, null);

    return LoginResponse.from(user);
}

private void handleFailedAttempt(User user) {
    int attempts = user.getFailedLoginAttempts() + 1;
    user.setFailedLoginAttempts(attempts);
    if (attempts >= 5) {
        user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
        notificationService.sendAccountLockedEmail(user);
    }
    userRepository.save(user);
}
Auth Sequence Diagram
sequenceDiagram
    participant B as Browser (Angular)
    participant API as Spring Boot /api/auth
    participant DB as MS SQL Server
    participant NS as NotificationService

    %% ── Normal Login ──────────────────────────────────────────────────────
    B->>API: POST /api/auth/login {email, password}
    API->>DB: SELECT user WHERE email = ?
    alt User not found
        API-->>B: 401 "Invalid credentials"
    else User found
        API->>API: bcrypt.verify(password, hash)
        alt Password wrong
            API->>DB: UPDATE failed_login_attempts++
            alt attempts >= 5
                API->>DB: SET locked_until = now + 15min
                API->>NS: send account-locked email
            end
            API-->>B: 401 "Invalid credentials"
        else Password correct
            API->>DB: RESET failed_login_attempts = 0
            API->>API: JwtService.generateToken(user) [RS256]
            API->>API: Set-Cookie: tms_jwt=<token>; HttpOnly; Secure; SameSite=Strict
            API->>DB: INSERT audit_log (LOGIN)
            API-->>B: 200 {userId, fullName, roles, timezone, forcePasswordChange}
        end
    end

    %% ── First Login — Forced Password Change ──────────────────────────────
    Note over B,API: forcePasswordChange = true in login response
    B->>B: Router redirects to /auth/change-password (cannot navigate away)
    B->>API: POST /api/auth/change-password {currentPassword, newPassword}
    API->>API: Validate new password complexity
    API->>DB: UPDATE password_hash, SET force_password_change = false
    API->>DB: INSERT audit_log (PASSWORD_CHANGED)
    API-->>B: 200 OK
    B->>B: Router redirects to role-appropriate dashboard

    %% ── Silent Token Refresh ───────────────────────────────────────────────
    Note over B,API: JWT interceptor catches 401 on any request
    B->>API: POST /api/auth/refresh (existing cookie sent automatically)
    API->>API: Validate existing JWT (not blacklisted, not expired by > grace period)
    API->>API: Issue new JWT
    API->>API: Set-Cookie: tms_jwt=<new-token>; HttpOnly; Secure; SameSite=Strict
    API-->>B: 204 No Content
    B->>B: Retry original failed request

    %% ── Logout ─────────────────────────────────────────────────────────────
    B->>API: POST /api/auth/logout
    API->>DB: INSERT token_blacklist (jti)
    API->>API: Clear cookie (Set-Cookie: tms_jwt=; Max-Age=0)
    API->>DB: INSERT audit_log (LOGOUT)
    API-->>B: 204 No Content
    B->>B: Clear NgRx auth state, navigate to /auth/login

    %% ── Forgot Password ────────────────────────────────────────────────────
    B->>API: POST /api/auth/forgot-password {email}
    API->>DB: SELECT user WHERE email = ?
    alt User exists
        API->>API: Generate secure random token
        API->>DB: INSERT password_reset_tokens (token_hash, expires_at = now+1hr)
        API->>NS: send password-reset email with link
    end
    API-->>B: 200 "If that email exists, a reset link has been sent."
    Note over API,B: Always 200 — prevents user enumeration

    %% ── Reset Password ─────────────────────────────────────────────────────
    B->>API: POST /api/auth/reset-password {token, newPassword}
    API->>DB: SELECT token WHERE token_hash = bcrypt(token) AND used=0 AND expires_at > now
    alt Token invalid/expired
        API-->>B: 400 "Reset token is invalid or has expired."
    else Token valid
        API->>DB: UPDATE user SET password_hash = bcrypt(newPassword)
        API->>DB: UPDATE token SET used = 1
        API->>DB: INSERT audit_log (PASSWORD_RESET)
        API-->>B: 200 "Password reset successfully. Please log in."
    end
Timesheet (/api/timesheets)
Method	Path	Description	Roles
GET	/api/timesheets/week	Get weekly summary for current user	Employee+
GET	/api/timesheets/week?weekStart={date}	Get weekly summary for a specific week	Employee+
GET	/api/timesheets/day/{date}	Get all task entries for a specific day	Employee+
POST	/api/timesheets/entries	Submit one or more task entries	Employee+
PUT	/api/timesheets/entries/{id}	Edit a PENDING/CLARIFICATION entry	Employee+
DELETE	/api/timesheets/entries/{id}	Delete a PENDING entry	Employee+
GET	/api/timesheets/history	Paginated history with filters	Employee+
GET	/api/timesheets/dashboard	Employee dashboard KPIs	Employee+
GET	/api/timesheets/export	Export filtered entries to CSV	Employee+
Approval (/api/approvals)
Method	Path	Description	Roles
GET	/api/approvals/team	Get team pending approvals summary	Manager
GET	/api/approvals/team/{employeeId}/week	Get employee weekly view for review	Manager
POST	/api/approvals/entries/{id}/approve	Approve a single task entry	Manager
POST	/api/approvals/entries/{id}/reject	Reject a single task entry with reason	Manager
POST	/api/approvals/entries/{id}/clarify	Request clarification on a task entry	Manager
POST	/api/approvals/day/{employeeId}/{date}/approve	Bulk approve all PENDING tasks for a day	Manager
POST	/api/approvals/day/{employeeId}/{date}/reject	Bulk reject all PENDING tasks for a day	Manager
POST	/api/approvals/week/{employeeId}/approve	Bulk approve all PENDING tasks for a week	Manager
Clarification (/api/clarifications)
Method	Path	Description	Roles
GET	/api/clarifications/entries/{entryId}	Get clarification thread for an entry	Employee+, Manager
POST	/api/clarifications/entries/{entryId}	Post a message to the thread	Employee+, Manager
Manager (/api/manager)
Method	Path	Description	Roles
GET	/api/manager/dashboard	Manager dashboard KPIs	Manager
GET	/api/manager/team	List direct reports with status summary	Manager
POST	/api/manager/reminders/missing	Send missing-entry reminder to direct reports	Manager
POST	/api/manager/reminders/employee/{id}	Send reminder to a specific employee	Manager
HR (/api/hr)
Method	Path	Description	Roles
GET	/api/hr/dashboard	Org-wide compliance KPIs	HR
GET	/api/hr/employees/{id}/daily-summary	Employee daily summary (no task detail)	HR
POST	/api/hr/reminders/missing	Send missing-entry reminder org-wide	HR
POST	/api/hr/reminders/pending-approvals	Send pending-approval reminder to managers	HR
GET	/api/hr/reports	List available reports	HR
POST	/api/hr/reports/generate	Trigger async report generation	HR
GET	/api/hr/reports/exports/{jobId}/status	Poll export job status	HR
GET	/api/hr/reports/exports/{jobId}/download	Download completed export	HR
GET	/api/hr/holidays	List holiday calendar entries	HR, Admin, Employee (read)
POST	/api/hr/holidays	Create holiday entry	HR, Admin
PUT	/api/hr/holidays/{id}	Update holiday entry	HR, Admin
DELETE	/api/hr/holidays/{id}	Delete holiday entry	HR, Admin
POST	/api/hr/holidays/import	Bulk import holidays via CSV	HR, Admin
Admin (/api/admin)
Method	Path	Description	Roles
GET	/api/admin/users	List users with search/filter	Admin
POST	/api/admin/users	Create user	Admin
PUT	/api/admin/users/{id}	Update user	Admin
POST	/api/admin/users/{id}/deactivate	Deactivate user	Admin
POST	/api/admin/users/{id}/reactivate	Reactivate user	Admin
POST	/api/admin/users/{id}/reset-password	Trigger password reset	Admin
POST	/api/admin/users/import	Bulk import users via CSV	Admin
GET	/api/admin/projects	List projects	Admin
POST	/api/admin/projects	Create project	Admin
PUT	/api/admin/projects/{id}	Update project	Admin
POST	/api/admin/projects/{id}/archive	Archive project	Admin
POST	/api/admin/projects/{id}/restore	Restore archived project	Admin
GET	/api/admin/manager-assignments	List all manager assignments	Admin
POST	/api/admin/manager-assignments	Create/update manager assignment	Admin
GET	/api/admin/org-chart	Full org chart	Admin
GET	/api/admin/config	Get system configuration	Admin
PUT	/api/admin/config	Update system configuration	Admin
GET	/api/admin/audit-log	Query audit log with filters	Admin
GET	/api/admin/audit-log/export	Export audit log to CSV	Admin
POST	/api/admin/email/test	Send test email	Admin
Notifications (/api/notifications)
Method	Path	Description	Roles
GET	/api/notifications	Get last 20 notifications for current user	Authenticated
POST	/api/notifications/{id}/read	Mark notification as read	Authenticated
POST	/api/notifications/read-all	Mark all notifications as read	Authenticated
Frontend Module Structure
src/app/
├── core/
│   ├── auth/
│   │   ├── auth.service.ts           # Login, logout, token refresh
│   │   ├── auth.guard.ts             # Route guard: authenticated
│   │   ├── role.guard.ts             # Route guard: role-based
│   │   └── jwt.interceptor.ts        # Attaches credentials, handles 401
│   ├── services/
│   │   ├── notification.service.ts   # WebSocket + REST notifications
│   │   ├── timezone.service.ts       # Browser timezone detection + override
│   │   └── api.service.ts            # Base HTTP service
│   └── models/                       # TypeScript interfaces matching DTOs
├── shared/
│   ├── components/
│   │   ├── shell/                    # App shell: header, sidebar, breadcrumb
│   │   ├── skeleton/                 # Skeleton screen components
│   │   ├── confirmation-dialog/
│   │   ├── toast/
│   │   └── empty-state/
│   ├── directives/
│   └── pipes/
│       ├── local-date.pipe.ts        # UTC → user local timezone
│       └── relative-time.pipe.ts
├── features/
│   ├── auth/                         # Lazy-loaded, no shell wrapper (full-page layout)
│   │   ├── login/
│   │   │   ├── login.component.ts    # Login form logic
│   │   │   └── login.component.html  # Login page template
│   │   ├── forgot-password/
│   │   │   ├── forgot-password.component.ts
│   │   │   └── forgot-password.component.html
│   │   ├── reset-password/
│   │   │   ├── reset-password.component.ts   # Consumes token from URL query param
│   │   │   └── reset-password.component.html
│   │   ├── change-password/
│   │   │   ├── change-password.component.ts  # First-login forced change + voluntary
│   │   │   └── change-password.component.html
│   │   └── auth.routes.ts
│   ├── employee/                     # Lazy-loaded
│   │   ├── dashboard/
│   │   ├── log-time/
│   │   ├── weekly-view/
│   │   ├── history/
│   │   └── employee.routes.ts
│   ├── manager/                      # Lazy-loaded
│   │   ├── dashboard/
│   │   ├── team-review/
│   │   ├── clarification-thread/
│   │   ├── team-reports/
│   │   └── manager.routes.ts
│   ├── hr/                           # Lazy-loaded
│   │   ├── dashboard/
│   │   ├── reminders/
│   │   ├── holiday-calendar/
│   │   ├── reports/
│   │   └── hr.routes.ts
│   └── admin/                        # Lazy-loaded
│       ├── users/
│       ├── projects/
│       ├── manager-assignments/
│       ├── system-config/
│       ├── audit-log/
│       └── admin.routes.ts
└── app.routes.ts                     # Root routing with role guards
Angular State Management
NgRx is used for global state that is shared across components or requires complex side-effect management:

Store Slice	Contents	Updated By
auth	Current user, roles, active role context, timezone	Login/logout/role-switch actions
notifications	Unread count, notification list	WebSocket messages, REST fetch
timesheet	Current week entries, day statuses	Timesheet API responses
config	System config values (thresholds, toggles)	Admin config API
Component-local state (form state, UI toggles, loading flags) is managed with Angular signals or component-level state — not NgRx.

Auth Feature — Frontend Design
The auth feature is a standalone lazy-loaded module with its own full-page layout (no shell sidebar/header). All auth pages are publicly accessible except change-password, which requires an authenticated session.

Auth Routes
// features/auth/auth.routes.ts
export const authRoutes: Routes = [
  { path: 'login',           component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password',  component: ResetPasswordComponent },  // expects ?token=<value>
  {
    path: 'change-password',
    component: ChangePasswordComponent,
    canActivate: [AuthGuard]  // must be logged in; ForcePasswordChangeGuard redirects here if forcePasswordChange=true
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];
Login Page Layout
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│              [Company Logo]                                     │
│         Timesheet Management System                             │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Sign In                                │  │
│  │                                                           │  │
│  │  Email Address                                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ john.doe@company.com                                │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                           │  │
│  │  Password                                          👁     │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ ••••••••••••                                        │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                           │  │
│  │  ⚠ [Inline error message — shown on failure]             │  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                   Sign In                           │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                           │  │
│  │                  Forgot your password?                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  © 2025 Think N Solutions                                       │
└─────────────────────────────────────────────────────────────────┘
Login Component Behaviour
// features/auth/login/login.component.ts
@Component({ selector: 'tms-login', standalone: true, ... })
export class LoginComponent {
  private authService = inject(AuthService);
  private router      = inject(Router);

  form = new FormGroup({
    email:    new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required])
  });

  loading  = signal(false);
  errorMsg = signal<string | null>(null);
  showPwd  = signal(false);

  async onSubmit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMsg.set(null);

    try {
      const response = await this.authService.login(this.form.value);

      // Store user context in NgRx auth store
      this.store.dispatch(AuthActions.loginSuccess({ user: response }));

      if (response.forcePasswordChange) {
        // Redirect to forced password change — cannot navigate away
        this.router.navigate(['/auth/change-password']);
      } else {
        // Redirect to role-appropriate dashboard
        this.router.navigate([this.authService.getRoleDashboardRoute(response.activeRole)]);
      }
    } catch (err: any) {
      if (err.status === 423) {
        this.errorMsg.set(`Account locked until ${formatLockedUntil(err.error.lockedUntil)}.`);
      } else {
        this.errorMsg.set('Invalid email or password.');  // Generic — never reveals which field
      }
    } finally {
      this.loading.set(false);
    }
  }
}
UX rules for the login page: - Submit button shows a spinner and is disabled while loading = true - Error message appears inline below the password field — not a toast or alert box - Password field has a show/hide toggle (👁 icon) - "Forgot your password?" is a plain text link — not a button - Pressing Enter in either field submits the form - On account lock, the error message shows the unlock time in the user's local timezone - No indication of whether the email or password was wrong (prevents user enumeration) - After 3 failed attempts (before lockout), a subtle hint is shown: "Having trouble? Check your caps lock."

Forgot Password Page
┌─────────────────────────────────────────────────────────────────┐
│  ← Back to Sign In                                              │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Reset your password                          │  │
│  │                                                           │  │
│  │  Enter your work email and we'll send you a reset link.   │  │
│  │                                                           │  │
│  │  Email Address                                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ john.doe@company.com                                │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │               Send Reset Link                       │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                           │  │
│  │  [Success state — replaces form after submit]             │  │
│  │  ✅ Check your inbox. If that email is registered,        │  │
│  │     a reset link has been sent. Link expires in 1 hour.   │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
On submit, the form is replaced by the success message regardless of whether the email exists (prevents enumeration)
The reset link in the email deep-links to /auth/reset-password?token=<value>
Reset Password Page
Reads token from the URL query parameter on component init
Validates the token with the backend on load (GET /api/auth/validate-reset-token?token=<value>) — shows an error immediately if the token is invalid or expired, before the user types anything
Password field + confirm password field with real-time complexity indicator (strength bar)
On success, redirects to /auth/login with a success toast: "Password reset. Please sign in."
Change Password Page (First Login + Voluntary)
If forcePasswordChange = true in the auth store: the back button and sidebar are hidden; the user cannot navigate away until the password is changed
Shows current password field + new password field + confirm field
Real-time password complexity indicator showing which rules are met:
✅ At least 8 characters
✅ One uppercase letter
✅ One number
✅ One special character
On success for forced change: clears forcePasswordChange flag in auth store, redirects to role dashboard
On success for voluntary change: shows success toast, stays on profile page
AuthService (core)
// core/auth/auth.service.ts
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http  = inject(HttpClient);
  private store = inject(Store);

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', credentials, {
      withCredentials: true  // required for HttpOnly cookie to be set/sent
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
      tap(() => this.store.dispatch(AuthActions.logout()))
    );
  }

  getRoleDashboardRoute(role: string): string {
    const routes: Record<string, string> = {
      ADMIN:    '/admin/dashboard',
      HR:       '/hr/dashboard',
      MANAGER:  '/manager/dashboard',
      EMPLOYEE: '/dashboard'
    };
    return routes[role] ?? '/dashboard';
  }
}
JWT Interceptor
// core/auth/jwt.interceptor.ts
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  // Always send cookies (JWT is HttpOnly — browser attaches it automatically)
  const authReq = req.clone({ withCredentials: true });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Attempt silent refresh
        return inject(AuthService).refresh().pipe(
          switchMap(() => next(authReq)),  // retry original request
          catchError(() => {
            // Refresh failed — session expired, redirect to login
            inject(Store).dispatch(AuthActions.sessionExpired());
            inject(Router).navigate(['/auth/login']);
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
ForcePasswordChangeGuard
// core/auth/force-password-change.guard.ts
export const forcePasswordChangeGuard: CanActivateFn = () => {
  const store  = inject(Store);
  const router = inject(Router);

  return store.select(selectForcePasswordChange).pipe(
    take(1),
    map(forceChange => {
      if (forceChange) {
        router.navigate(['/auth/change-password']);
        return false;
      }
      return true;
    })
  );
};
// Applied to ALL authenticated routes so the user cannot bypass the forced change
Data Models
Database Schema
All tables use UTC for date/timestamp columns. Flyway manages versioned migrations under src/main/resources/db/migration/.

Table: users
CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name       NVARCHAR(200)   NOT NULL,
    email           NVARCHAR(255)   NOT NULL UNIQUE,
    password_hash   NVARCHAR(255)   NOT NULL,
    employee_id     NVARCHAR(50)    NOT NULL UNIQUE,
    department      NVARCHAR(100)   NULL,
    timezone        NVARCHAR(50)    NOT NULL DEFAULT 'UTC',  -- IANA timezone ID
    status          NVARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','INACTIVE')),
    failed_login_attempts INT       NOT NULL DEFAULT 0,
    locked_until    DATETIME2       NULL,
    force_password_change BIT       NOT NULL DEFAULT 1,
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_users_email   ON users(email);
CREATE INDEX idx_users_status  ON users(status);
CREATE INDEX idx_users_dept    ON users(department);
Table: user_roles
CREATE TABLE user_roles (
    user_id BIGINT       NOT NULL REFERENCES users(id),
    role    NVARCHAR(20) NOT NULL CHECK (role IN ('EMPLOYEE','MANAGER','HR','ADMIN')),
    PRIMARY KEY (user_id, role)
);
Table: projects
CREATE TABLE projects (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(200)  NOT NULL UNIQUE,
    code        NVARCHAR(50)   NOT NULL UNIQUE,
    client      NVARCHAR(200)  NULL,
    department  NVARCHAR(100)  NULL,
    start_date  DATE           NOT NULL,
    end_date    DATE           NULL,
    status      NVARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE','ARCHIVED')),
    created_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_code   ON projects(code);
Table: project_assignments
-- Controls which employees see which projects in their dropdown
CREATE TABLE project_assignments (
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    assigned_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_proj_assign_user ON project_assignments(user_id);
Table: timesheet_entries
CREATE TABLE timesheet_entries (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                 BIGINT          NOT NULL REFERENCES users(id),
    project_id              BIGINT          NOT NULL REFERENCES projects(id),
    entry_date              DATE            NOT NULL,  -- stored as UTC calendar date
    task_name               NVARCHAR(100)   NOT NULL,
    task_description        NVARCHAR(500)   NULL,
    hours                   DECIMAL(4,1)    NOT NULL
                                CHECK (hours >= 0.5 AND hours <= 9.0),
    status                  NVARCHAR(30)    NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING','APPROVED','REJECTED',
                                                  'CLARIFICATION_REQUESTED','AUTO_APPROVED')),
    overtime_justification  NVARCHAR(300)   NULL,
    -- manager_id at time of submission (preserved across reassignments)
    assigned_manager_id     BIGINT          NULL REFERENCES users(id),
    is_weekend              BIT             NOT NULL DEFAULT 0,
    is_holiday              BIT             NOT NULL DEFAULT 0,
    submitted_at            DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

-- Core query patterns: employee's entries by date range
CREATE INDEX idx_te_user_date       ON timesheet_entries(user_id, entry_date);
-- Manager review: all entries for a manager's direct reports
CREATE INDEX idx_te_manager_date    ON timesheet_entries(assigned_manager_id, entry_date);
-- Status-based filtering
CREATE INDEX idx_te_status          ON timesheet_entries(status);
-- Project reporting
CREATE INDEX idx_te_project_date    ON timesheet_entries(project_id, entry_date);
-- Composite for daily total computation
CREATE INDEX idx_te_user_date_status ON timesheet_entries(user_id, entry_date, status);
Design notes: - entry_date is a DATE (not DATETIME2) — the calendar date in UTC. The API accepts the user's local date and converts to UTC date at the boundary. - hours is constrained to 0.5–9.0 at the DB level as a safety net; the API enforces the overtime justification requirement at 9+ hours. - assigned_manager_id is captured at submission time and never updated — this preserves the manager-reassignment edge case (Scenario 13/15). - status includes AUTO_APPROVED as a distinct value to support the "Auto-Approved" label in the UI and audit log. - Day_Status is not stored — it is computed on every read from the task-level statuses.

Table: approval_actions
CREATE TABLE approval_actions (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    entry_id    BIGINT          NOT NULL REFERENCES timesheet_entries(id),
    actor_id    BIGINT          NOT NULL REFERENCES users(id),
    action      NVARCHAR(30)    NOT NULL
                    CHECK (action IN ('APPROVED','REJECTED','CLARIFICATION_REQUESTED',
                                      'AUTO_APPROVED')),
    reason      NVARCHAR(500)   NULL,  -- mandatory for REJECTED
    created_at  DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_aa_entry_id  ON approval_actions(entry_id);
CREATE INDEX idx_aa_actor_id  ON approval_actions(actor_id);
CREATE INDEX idx_aa_created   ON approval_actions(created_at);
Table: manager_assignments
CREATE TABLE manager_assignments (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    employee_id     BIGINT      NOT NULL REFERENCES users(id),
    manager_id      BIGINT      NOT NULL REFERENCES users(id),
    effective_from  DATETIME2   NOT NULL DEFAULT SYSUTCDATETIME(),
    effective_to    DATETIME2   NULL,  -- NULL = currently active
    CONSTRAINT chk_no_self_manage CHECK (employee_id <> manager_id)
);

-- Only one active assignment per employee at a time (enforced in service layer)
CREATE INDEX idx_ma_employee    ON manager_assignments(employee_id, effective_to);
CREATE INDEX idx_ma_manager     ON manager_assignments(manager_id, effective_to);
Design note: Circular assignment prevention (A manages B, B manages A) is enforced in ManagerAssignmentService by traversing the assignment graph before committing. The DB constraint only prevents direct self-assignment.

Table: clarification_messages
CREATE TABLE clarification_messages (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    entry_id    BIGINT          NOT NULL REFERENCES timesheet_entries(id),
    author_id   BIGINT          NOT NULL REFERENCES users(id),
    message     NVARCHAR(2000)  NOT NULL,
    created_at  DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_cm_entry_id ON clarification_messages(entry_id);
Table: audit_logs
CREATE TABLE audit_logs (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    actor_id        BIGINT          NULL REFERENCES users(id),  -- NULL for system actions
    action_type     NVARCHAR(100)   NOT NULL,
    entity_type     NVARCHAR(100)   NOT NULL,
    entity_id       BIGINT          NULL,
    before_value    NVARCHAR(MAX)   NULL,  -- JSON snapshot
    after_value     NVARCHAR(MAX)   NULL,  -- JSON snapshot
    ip_address      NVARCHAR(45)    NULL,
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

-- Audit log is append-only; no UPDATE or DELETE permissions granted to app user
CREATE INDEX idx_al_actor       ON audit_logs(actor_id);
CREATE INDEX idx_al_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_al_created     ON audit_logs(created_at);
CREATE INDEX idx_al_action_type ON audit_logs(action_type);
Table: notifications
CREATE TABLE notifications (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    type        NVARCHAR(50)    NOT NULL,
    message     NVARCHAR(500)   NOT NULL,
    is_read     BIT             NOT NULL DEFAULT 0,
    deep_link   NVARCHAR(500)   NULL,
    created_at  DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_notif_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notif_created   ON notifications(created_at);
Table: reminder_logs
CREATE TABLE reminder_logs (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    sent_by          BIGINT          NULL REFERENCES users(id),  -- NULL = automated
    sender_role      NVARCHAR(20)    NOT NULL
                         CHECK (sender_role IN ('SYSTEM','HR','MANAGER')),
    reminder_type    NVARCHAR(50)    NOT NULL,
    recipient_type   NVARCHAR(50)    NOT NULL,
    recipient_count  INT             NOT NULL DEFAULT 0,
    sent_at          DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);
Table: system_config
CREATE TABLE system_config (
    config_key      NVARCHAR(100)   NOT NULL PRIMARY KEY,
    config_value    NVARCHAR(500)   NOT NULL,
    updated_by      BIGINT          NULL REFERENCES users(id),
    updated_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

-- Seed data (inserted via Flyway migration)
-- work_week_start: 'MONDAY'
-- daily_warning_hours: '8'
-- daily_overtime_threshold: '9'
-- reminder_time: '17:00'
-- reminder_days: 'MON,TUE,WED,THU,FRI'
-- past_entry_edit_window_days: '30'
-- lock_period_days: '0'  (0 = disabled)
-- weekend_logging_enabled: 'true'
Table: holiday_calendar
CREATE TABLE holiday_calendar (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(200)   NOT NULL,
    holiday_date    DATE            NOT NULL,
    type            NVARCHAR(30)    NOT NULL
                        CHECK (type IN ('PUBLIC_HOLIDAY','COMPANY_HOLIDAY','OPTIONAL_HOLIDAY')),
    applicable_to   NVARCHAR(50)    NOT NULL DEFAULT 'ALL',
                        -- 'ALL' or comma-separated department names
    created_by      BIGINT          NOT NULL REFERENCES users(id),
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX idx_hc_date_dept ON holiday_calendar(holiday_date, applicable_to);
CREATE INDEX idx_hc_date ON holiday_calendar(holiday_date);
Table: password_reset_tokens
CREATE TABLE password_reset_tokens (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    token_hash  NVARCHAR(255)   NOT NULL UNIQUE,  -- bcrypt hash of the token
    expires_at  DATETIME2       NOT NULL,
    used        BIT             NOT NULL DEFAULT 0,
    created_at  DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_prt_token_hash ON password_reset_tokens(token_hash);
Table: export_jobs
CREATE TABLE export_jobs (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    requested_by    BIGINT          NOT NULL REFERENCES users(id),
    job_type        NVARCHAR(50)    NOT NULL,
    status          NVARCHAR(20)    NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    file_path       NVARCHAR(500)   NULL,
    error_message   NVARCHAR(1000)  NULL,
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    completed_at    DATETIME2       NULL
);

CREATE INDEX idx_ej_user_status ON export_jobs(requested_by, status);
Flyway Migration Strategy
Migrations live in src/main/resources/db/migration/ following the naming convention V{version}__{description}.sql:

V1__create_users_and_roles.sql
V2__create_projects_and_assignments.sql
V3__create_timesheet_entries.sql
V4__create_approval_actions.sql
V5__create_manager_assignments.sql
V6__create_clarification_messages.sql
V7__create_audit_logs.sql
V8__create_notifications.sql
V9__create_reminder_logs.sql
V10__create_system_config.sql
V11__create_holiday_calendar.sql
V12__create_password_reset_tokens.sql
V13__create_export_jobs.sql
V14__seed_system_config_defaults.sql
Flyway runs automatically on application startup. The application DB user has DDL rights only during migration; the runtime user has DML rights only (SELECT, INSERT, UPDATE, DELETE on specific tables — no DROP, no TRUNCATE on audit_logs).

Key Workflows
1. Task Submission → Auto-Approval Check → Manager Notification
sequenceDiagram
    participant E as Employee (Angular)
    participant API as Spring Boot API
    participant DB as MS SQL Server
    participant NS as NotificationService
    participant SMTP as Email Server
    participant WS as WebSocket

    E->>API: POST /api/timesheets/entries [{task entries}]
    API->>API: Validate inputs (mandatory fields, date not future, not > 30 days past)
    API->>DB: SELECT SUM(hours) for user+date (existing entries)
    API->>API: OvertimeValidator: check daily total
    alt Daily total > 9 hrs AND no overtime_justification
        API-->>E: 400 Bad Request: "Please provide a reason for logging more than 9 hours"
    else Valid submission
        loop For each task entry
            alt hours < 1 (0.5 hr)
                API->>DB: INSERT timesheet_entry (status=AUTO_APPROVED)
                API->>DB: INSERT approval_action (action=AUTO_APPROVED, actor=SYSTEM)
                API->>DB: INSERT audit_log
            else hours >= 1
                API->>DB: INSERT timesheet_entry (status=PENDING, assigned_manager_id=current_manager)
                API->>DB: INSERT audit_log
                API->>NS: createNotification(manager, "New submission from {employee}")
                NS->>DB: INSERT notification (manager)
                NS->>WS: push notification to manager
                NS->>SMTP: send email to manager
            end
        end
        API-->>E: 201 Created [{entry responses}]
        API->>NS: createNotification(employee, "Submission confirmed")
        NS->>WS: push toast to employee
    end
2. Manager Approval / Rejection Flow
sequenceDiagram
    participant M as Manager (Angular)
    participant API as Spring Boot API
    participant DB as MS SQL Server
    participant NS as NotificationService

    M->>API: POST /api/approvals/entries/{id}/approve
    API->>DB: SELECT timesheet_entry WHERE id={id}
    API->>API: Check: actor != entry.user_id (no self-approval)
    API->>API: Check: actor is assigned_manager_id OR skip-level/HR
    API->>DB: UPDATE timesheet_entry SET status=APPROVED
    API->>DB: INSERT approval_action (APPROVED)
    API->>DB: INSERT audit_log
    API->>API: DayStatusComputer.compute(user_id, entry_date)
    API->>NS: notify employee (APPROVED)
    NS-->>M: 200 OK {updated entry + day_status}

    Note over M,NS: Rejection follows same flow with mandatory reason
    M->>API: POST /api/approvals/entries/{id}/reject {reason}
    API->>API: Validate reason.length >= 10
    API->>DB: UPDATE timesheet_entry SET status=REJECTED
    API->>DB: INSERT approval_action (REJECTED, reason)
    API->>NS: notify employee (REJECTED with reason)
3. Clarification Thread Flow
sequenceDiagram
    participant M as Manager
    participant API as Spring Boot API
    participant E as Employee
    participant NS as NotificationService

    M->>API: POST /api/approvals/entries/{id}/clarify {message}
    API->>DB: UPDATE timesheet_entry SET status=CLARIFICATION_REQUESTED
    API->>DB: INSERT clarification_message (author=manager)
    API->>DB: INSERT audit_log
    API->>NS: notify employee (clarification requested)
    NS->>E: email + in-app notification

    E->>API: POST /api/clarifications/entries/{id} {reply message}
    API->>DB: INSERT clarification_message (author=employee)
    API->>NS: notify manager (employee replied)
    NS->>M: email + in-app notification

    M->>API: POST /api/approvals/entries/{id}/approve (from thread)
    API->>DB: UPDATE timesheet_entry SET status=APPROVED
    API->>DB: INSERT clarification_message is now read-only
    Note over M,E: Thread becomes read-only after APPROVED/REJECTED
4. Missed-Date Detection and Reminder Flow
sequenceDiagram
    participant Sched as MissedDateDetectionJob
    participant DB as MS SQL Server
    participant TZ as TimezoneUtil
    participant NS as NotificationService

    Note over Sched: Runs every minute; checks per-user 5 PM trigger
    Sched->>DB: SELECT users WHERE status=ACTIVE
    loop For each active user
        Sched->>TZ: isNow5PMInUserTimezone(user.timezone)
        alt It is 5 PM for this user
            Sched->>DB: SELECT today's date in user's timezone
            Sched->>DB: SELECT holiday_calendar WHERE date=today AND applicable_to covers user
            alt Today is a holiday
                Note over Sched: Skip — no missed-date check
            else Today is Mon-Fri (work day)
                Sched->>DB: SELECT COUNT(*) FROM timesheet_entries WHERE user_id=? AND entry_date=today
                alt No entries found
                    Sched->>DB: INSERT notification (employee: missed date)
                    Sched->>NS: send email to employee + manager
                    Sched->>DB: INSERT reminder_log
                end
            end
        end
    end
Implementation note: Rather than running a cron at exactly 5 PM (which would require per-timezone crons), the scheduler runs every minute and checks whether the current UTC time corresponds to 5 PM in each user's timezone. This is O(n) per minute but efficient with proper indexing. For large user bases, users can be batched by timezone offset.

5. Overtime Submission Flow
sequenceDiagram
    participant E as Employee
    participant UI as Angular Log Time Component
    participant API as Spring Boot API
    participant M as Manager (Review UI)

    E->>UI: Adds tasks; running total reaches 8 hrs
    UI->>UI: Show soft warning banner (>8 hrs)
    E->>UI: Adds another task; total reaches 9+ hrs
    UI->>UI: Show prominent warning + overtime_justification field
    UI->>UI: Disable Submit button until justification entered (min 10 chars)
    E->>UI: Enters justification, clicks Submit
    E->>API: POST /api/timesheets/entries [{tasks, overtime_justification}]
    API->>API: OvertimeValidator: total > 9, justification present and valid
    API->>DB: INSERT entries with overtime_justification stored
    API->>DB: INSERT audit_log

    Note over M: No email/notification sent for overtime
    M->>API: GET /api/approvals/team/{employeeId}/week
    API->>DB: SELECT entries + compute daily totals
    API->>API: Flag days where total > overtime_threshold
    API-->>M: {week data, days: [{date, total_hours, has_overtime: true, overtime_justification}]}
    M->>UI: Day column shows amber ⚠️ badge
    M->>UI: Expands day → tasks highlighted with amber left-border + justification shown inline
6. Holiday Calendar Impact
sequenceDiagram
    participant HR as HR User
    participant API as Spring Boot API
    participant DB as MS SQL Server
    participant Sched as Scheduler

    HR->>API: POST /api/hr/holidays {name, date, type, applicable_to}
    API->>DB: INSERT holiday_calendar
    API->>DB: INSERT audit_log

    Note over API,DB: Retroactive holiday edge case
    API->>DB: SELECT users WHERE missed_date_flag = date AND applicable
    API->>DB: Clear any pending missed-date notifications for that date
    API->>DB: Cancel pending reminder_log entries for that date

    Note over Sched: Next reminder cycle
    Sched->>DB: SELECT holidays WHERE date = today
    Sched->>Sched: Skip all users for holiday dates

    Note over HR: Holiday deletion
    HR->>API: DELETE /api/hr/holidays/{id}
    API->>DB: DELETE holiday_calendar
    API->>DB: INSERT audit_log
    Note over API: Dates re-evaluated at NEXT reminder cycle only (not retroactively)
Day_Status Computation Logic
DayStatusComputer is a stateless utility class called on every read that returns task-level entries for a day:

public enum DayStatus { PENDING, APPROVED, REJECTED, CLARIFICATION_REQUESTED }

public class DayStatusComputer {
    public static DayStatus compute(List<TimesheetEntry> dayEntries) {
        if (dayEntries.isEmpty()) return null; // no entries = no status

        boolean anyRejected = dayEntries.stream()
            .anyMatch(e -> e.getStatus() == EntryStatus.REJECTED);
        if (anyRejected) return DayStatus.REJECTED;

        boolean anyClarification = dayEntries.stream()
            .anyMatch(e -> e.getStatus() == EntryStatus.CLARIFICATION_REQUESTED);
        if (anyClarification) return DayStatus.CLARIFICATION_REQUESTED;

        boolean allApproved = dayEntries.stream()
            .allMatch(e -> e.getStatus() == EntryStatus.APPROVED
                        || e.getStatus() == EntryStatus.AUTO_APPROVED);
        if (allApproved) return DayStatus.APPROVED;

        return DayStatus.PENDING;
    }
}
Precedence: REJECTED > CLARIFICATION_REQUESTED > PENDING > APPROVED (i.e., APPROVED only when ALL tasks are approved/auto-approved).

Auto-Approval Logic
In TimesheetService.submitEntries():

for (TimesheetEntryRequest req : requests) {
    TimesheetEntry entry = mapToEntity(req);
    entry.setAssignedManagerId(resolveCurrentManager(entry.getUserId()));

    if (req.getHours().compareTo(BigDecimal.ONE) < 0) {
        // Auto-approve: hours < 1 (i.e., 0.5)
        entry.setStatus(EntryStatus.AUTO_APPROVED);
        auditLogService.log(SYSTEM, AUTO_APPROVED, entry);
        // No manager notification
    } else {
        entry.setStatus(EntryStatus.PENDING);
        auditLogService.log(actor, SUBMITTED, entry);
        notificationService.notifyManager(entry.getAssignedManagerId(), entry);
    }
    timesheetEntryRepository.save(entry);
}
When an auto-approved entry is edited to hours >= 1:

entry.setStatus(EntryStatus.PENDING);
entry.setAssignedManagerId(resolveCurrentManager(entry.getUserId())); // re-resolve manager
auditLogService.log(actor, EDITED_AUTO_APPROVED_RESET_TO_PENDING, entry);
notificationService.notifyManager(entry.getAssignedManagerId(), "Previously auto-approved entry modified");
Self-Approval Prevention
In ApprovalService.approve():

if (entry.getUserId().equals(actorId)) {
    // Route to skip-level manager
    Long skipLevelManagerId = managerAssignmentService.getManagerOf(actorId);
    if (skipLevelManagerId == null) {
        // No manager — route to any HR user
        skipLevelManagerId = userService.getAnyHrUserId();
    }
    // Re-assign entry to skip-level manager
    entry.setAssignedManagerId(skipLevelManagerId);
    timesheetEntryRepository.save(entry);
    throw new SelfApprovalException("Entry re-routed to skip-level manager");
}
Timezone Handling
All timezone logic is centralized in TimezoneUtil:

public class TimezoneUtil {
    // Convert user's local date to UTC date for storage
    public static LocalDate toUtcDate(LocalDate localDate, String ianaTimezone) {
        ZoneId zone = ZoneId.of(ianaTimezone);
        ZonedDateTime localMidnight = localDate.atStartOfDay(zone);
        return localMidnight.withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    // Check if current UTC time is 5 PM in user's timezone
    public static boolean isEodReminderTime(String ianaTimezone) {
        ZoneId zone = ZoneId.of(ianaTimezone);
        LocalTime nowInZone = Instant.now().atZone(zone).toLocalTime();
        return nowInZone.getHour() == 17 && nowInZone.getMinute() == 0;
    }
}
The API accepts a X-User-Timezone header on all timesheet write operations. If absent, the user's stored timezone from the users table is used as fallback.

Security Design
JWT Authentication
Algorithm: RS256 (asymmetric). Private key signs tokens; public key verifies. Keys stored as environment variables / secrets manager — never in source code.
Storage: JWT stored in HttpOnly, Secure, SameSite=Strict cookie. Never in localStorage or sessionStorage.
Expiry: 8-hour access token. Silent refresh via /api/auth/refresh when the user is active (Angular interceptor detects 401 and retries after refresh).
Invalidation: On logout, the JWT jti (JWT ID) claim is stored in a TokenBlacklist (in-memory set backed by a DB table for multi-instance deployments). Every request checks the blacklist.
Claims: sub (user ID), roles (list), jti (unique token ID), iat, exp.
Spring Security Configuration
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // JWT filter chain
    // CORS: allow only configured frontend origin
    // CSRF: double-submit cookie pattern (not disabled)
    // Session: STATELESS
    // Public endpoints: /api/auth/login, /api/auth/forgot-password, /api/auth/reset-password
    // All others: authenticated
}
Method-level security with @PreAuthorize:

@PreAuthorize("hasRole('MANAGER')")
public ApprovalResult approveEntry(Long entryId, Long actorId) { ... }

@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public void createHoliday(HolidayRequest request) { ... }
RBAC Enforcement
Role	Can Access
EMPLOYEE	Own timesheet entries, own notifications, own profile, holiday calendar (read)
MANAGER	All EMPLOYEE access + direct reports' entries (read/approve), team reports, send reminders
HR	All EMPLOYEE access + all employees' daily summaries (no task detail), org reports, holiday calendar (write), send org-wide reminders
ADMIN	All access + user management, project management, system config, audit log, manager assignments
Key constraint: HR can see aggregated daily summaries but NOT individual task descriptions. This is enforced at the service layer by returning DailySummaryDto (hours per day) instead of TimesheetEntryDto (task detail) for HR queries.

OWASP Top 10 Mitigations
Risk	Mitigation
A01 Broken Access Control	RBAC on every endpoint; @PreAuthorize; ownership checks (user can only edit own entries)
A02 Cryptographic Failures	TLS 1.2+; bcrypt (cost 12) for passwords; RS256 JWT; HttpOnly cookies
A03 Injection	Spring Data JPA with parameterized queries; no native SQL string concatenation; input validation via Bean Validation
A04 Insecure Design	Threat modeling per feature; self-approval prevention; circular assignment prevention
A05 Security Misconfiguration	CORS restricted to frontend origin; security headers (CSP, HSTS, X-Frame-Options) via Spring Security
A06 Vulnerable Components	Dependency scanning via OWASP Dependency-Check in CI pipeline
A07 Auth Failures	Account lockout after 5 failed attempts; single-use password reset tokens; forced password change on first login
A08 Software Integrity	Signed JWT (RS256); Flyway checksums on migrations
A09 Logging Failures	Audit log for all state changes; sensitive fields masked in application logs
A10 SSRF	No user-controlled URL fetching; SMTP host configured server-side only
Frontend Design Details
Shell Layout
┌─────────────────────────────────────────────────────────────────┐
│ TOP HEADER (fixed, full-width, deep navy)                       │
│ [Logo/TMS]    [Global Search]    [🔔 3] [Role Switcher] [👤 ▼] │
├──────────────┬──────────────────────────────────────────────────┤
│ LEFT SIDEBAR │ MAIN CONTENT AREA                                │
│ (collapsible)│ Breadcrumb: My Team > John Doe > Week Apr 14     │
│              │ ─────────────────────────────────────────────── │
│ 📊 Dashboard │                                                  │
│ ⏱ Log Time  │   [Page Content]                                 │
│ 📋 Timesheets│                                                  │
│   Weekly View│                                                  │
│   History    │                                                  │
│ 👤 Profile   │                                                  │
│              │ ─────────────────────────────────────────────── │
│ [Role-extra] │ CONTEXTUAL ACTION BAR (sticky bottom)           │
│              │ [Cancel]  [Save Draft]  [Submit]                │
└──────────────┴──────────────────────────────────────────────────┘
Role-Driven Routing
// app.routes.ts
export const routes: Routes = [
  { path: 'auth', loadChildren: () => import('./features/auth/auth.routes') },
  {
    path: '',
    component: ShellComponent,
    canActivate: [AuthGuard],
    children: [
      // Employee routes (all authenticated users)
      { path: 'dashboard', loadChildren: () => import('./features/employee/employee.routes') },
      { path: 'log-time', loadChildren: () => import('./features/employee/employee.routes') },
      { path: 'timesheets', loadChildren: () => import('./features/employee/employee.routes') },
      // Manager routes
      {
        path: 'manager',
        canActivate: [RoleGuard],
        data: { roles: ['MANAGER'] },
        loadChildren: () => import('./features/manager/manager.routes')
      },
      // HR routes
      {
        path: 'hr',
        canActivate: [RoleGuard],
        data: { roles: ['HR'] },
        loadChildren: () => import('./features/hr/hr.routes')
      },
      // Admin routes
      {
        path: 'admin',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] },
        loadChildren: () => import('./features/admin/admin.routes')
      },
      // Catch-all: redirect to role-appropriate dashboard
      { path: '**', component: RoleRedirectComponent }
    ]
  }
];
RoleGuard redirects to the user's role-appropriate dashboard (not a 403 page) if the user lacks the required role. The sidebar never renders links to routes the user cannot access.

Key Component Behaviors
1. Log Time Component (Employee)
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ 📊 Dashboard  ⏱ Log Time  📋 My Timesheets  👤 Profile             │ ← Sidebar
├─────────────────────────────────────────────────────────────────────┤
│ Log Time  >  Monday, Apr 14, 2025  (IST — UTC+5:30)                │ ← Breadcrumb + TZ
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌── Task Entry Form ─────────────────────────────────────────────┐ │
│  │  Project *          Task Name *              Hours *           │ │
│  │  [Dropdown ▼]       [Free text, 100 chars]   [▼ 4.0 ▲]        │ │
│  │                                                                │ │
│  │  Task Description (optional)                                   │ │
│  │  [Free text area, 500 chars]                                   │ │
│  │                                                                │ │
│  │                                          [+ Add Another Task]  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌── Task Entry Form (2nd task) ──────────────────────────────────┐ │
│  │  Project *          Task Name *              Hours *           │ │
│  │  [Dropdown ▼]       [Free text]              [▼ 3.0 ▲]        │ │
│  │                                                    [✕ Remove]  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  Daily Total: 7.0 hrs  ████████████░░░░  (of 8 hrs standard)       │ ← Progress bar
│                                                                     │
│  ⚠ [Amber banner — appears at 8+ hrs]                              │
│  ⛔ [Red banner + justification field — appears at 9+ hrs]          │
│                                                                     │
│  Overtime Justification (required when total > 9 hrs)              │
│  [Text area, min 10 / max 300 chars]                               │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│ [Cancel]                                    [Save Draft]  [Submit] │ ← Sticky action bar
└─────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// POST /api/timesheets/entries
// Request
{
  "date": "2025-04-14",
  "overtimeJustification": null,
  "entries": [
    {
      "projectId": 3,
      "taskName": "Backend API development",
      "taskDescription": "Implemented approval endpoints",
      "hours": 4.0
    },
    {
      "projectId": 5,
      "taskName": "Code review",
      "taskDescription": null,
      "hours": 3.0
    }
  ]
}

// Response 201 Created
{
  "date": "2025-04-14",
  "dailyTotal": 7.0,
  "dayStatus": "PENDING",
  "entries": [
    {
      "id": 101,
      "projectId": 3,
      "projectName": "TMS Project",
      "taskName": "Backend API development",
      "hours": 4.0,
      "status": "PENDING",
      "submittedAt": "2025-04-14T08:30:00Z"
    },
    {
      "id": 102,
      "projectId": 5,
      "projectName": "Internal Tools",
      "taskName": "Code review",
      "hours": 3.0,
      "status": "PENDING",
      "submittedAt": "2025-04-14T08:30:00Z"
    }
  ]
}

// Response 400 — overtime justification missing
{
  "error": "OVERTIME_JUSTIFICATION_REQUIRED",
  "message": "Please provide a reason for logging more than 9 hours.",
  "dailyTotal": 9.5
}
Component Code
// features/employee/log-time/log-time.component.ts
@Component({ selector: 'tms-log-time', standalone: true })
export class LogTimeComponent {
  private timesheetService = inject(TimesheetService);
  private timezoneService  = inject(TimezoneService);

  today        = signal(new Date());
  timezone     = signal(this.timezoneService.getActiveTimezone());
  tasks        = signal<TaskEntryForm[]>([this.emptyTask()]);
  dailyTotal   = computed(() => this.tasks().reduce((s, t) => s + (t.hours ?? 0), 0));
  showWarning  = computed(() => this.dailyTotal() >= 8 && this.dailyTotal() < 9);
  showOvertime = computed(() => this.dailyTotal() >= 9);
  loading      = signal(false);

  overtimeJustification = new FormControl('', [
    Validators.minLength(10),
    Validators.maxLength(300)
  ]);

  canSubmit = computed(() =>
    this.tasks().every(t => t.projectId && t.taskName && t.hours) &&
    (!this.showOvertime() || (this.overtimeJustification.value?.length ?? 0) >= 10)
  );

  addTask()         { this.tasks.update(t => [...t, this.emptyTask()]); }
  removeTask(i: number) { this.tasks.update(t => t.filter((_, idx) => idx !== i)); }

  async submit() {
    if (!this.canSubmit()) return;
    this.loading.set(true);
    try {
      await firstValueFrom(this.timesheetService.submitEntries({
        date: formatLocalDate(this.today(), this.timezone()),
        overtimeJustification: this.showOvertime() ? this.overtimeJustification.value : null,
        entries: this.tasks()
      }));
      // Reset form, show success toast
      this.tasks.set([this.emptyTask()]);
      this.overtimeJustification.reset();
    } finally {
      this.loading.set(false);
    }
  }

  private emptyTask = (): TaskEntryForm => ({ projectId: null, taskName: '', taskDescription: '', hours: null });
}
UX Rules
Date defaults to today in user's local timezone; cannot be set to a future date
Progress bar fills from 0 to 8 hrs (standard); turns amber at 8 hrs, red at 9+ hrs
Overtime justification field slides in with animation when total crosses 9 hrs; Submit stays disabled until min 10 chars entered
Each task row has a "✕ Remove" button; minimum 1 task row always present
Project dropdown shows only active projects assigned to the employee
Hours stepper: 0.5 increments, keyboard-editable, min 0.5 max 9.0 per task
On successful submit: success toast "X tasks logged for [date]", form resets
2. Weekly View Component (Employee)
Page Layout
┌─────────────────────────────────────────────────────────────────────────────────┐
│ My Timesheets > Weekly View                          [← Prev Week] [Next Week →]│
│ Week of Apr 14 – Apr 20, 2025                                                   │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────────┤
│  MON 14  │  TUE 15  │  WED 16  │  THU 17  │  FRI 18  │  SAT 19  │    SUN 20   │
│          │          │          │          │          │ Weekend  │   Weekend    │
│  8.0 hrs │  7.5 hrs │  0 hrs   │  4.0 hrs │  0 hrs   │  3.0 hrs │   0 hrs     │
│ ●APPROVED│ ●PENDING │ ⚠MISSED  │ ●CLARIF. │ 🏖️Holiday│ ●PENDING │   (empty)   │
│  [+Add]  │  [+Add]  │[Log Now] │  [+Add]  │[Log Time]│  [+Add]  │   [+Add]    │
├──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────────┤
│ ▼ THU 17 — 4.0 hrs  [CLARIFICATION_REQUESTED]                                  │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │ Task: "Database schema review"  Project: TMS  2.0 hrs  🔵 CLARIF.      │   │
│   │ Task: "Sprint planning"         Project: TMS  2.0 hrs  ✅ APPROVED      │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/timesheets/week?weekStart=2025-04-14
// Response 200 OK
{
  "weekStart": "2025-04-14",
  "weekEnd":   "2025-04-20",
  "days": [
    {
      "date":       "2025-04-14",
      "dayOfWeek":  "MONDAY",
      "isWeekend":  false,
      "isHoliday":  false,
      "isMissed":   false,
      "totalHours": 8.0,
      "dayStatus":  "APPROVED",
      "entries": [
        {
          "id": 101, "taskName": "Backend API", "projectName": "TMS",
          "hours": 4.0, "status": "APPROVED", "isAutoApproved": false
        },
        {
          "id": 102, "taskName": "Code review", "projectName": "TMS",
          "hours": 4.0, "status": "APPROVED", "isAutoApproved": false
        }
      ]
    },
    {
      "date": "2025-04-18", "dayOfWeek": "FRIDAY",
      "isWeekend": false, "isHoliday": true,
      "holidayName": "Good Friday",
      "isMissed": false, "totalHours": 0, "dayStatus": null, "entries": []
    },
    {
      "date": "2025-04-19", "dayOfWeek": "SATURDAY",
      "isWeekend": true, "isHoliday": false,
      "isMissed": false, "totalHours": 3.0, "dayStatus": "PENDING", "entries": [...]
    }
  ],
  "weeklyTotal": 22.5
}
Component Code
// features/employee/weekly-view/weekly-view.component.ts
@Component({ selector: 'tms-weekly-view', standalone: true })
export class WeeklyViewComponent {
  private timesheetService = inject(TimesheetService);

  weekStart    = signal(startOfWeek(new Date(), { weekStartsOn: 1 })); // Monday
  weekData     = signal<WeekResponse | null>(null);
  expandedDay  = signal<string | null>(null);
  loading      = signal(false);

  ngOnInit() { this.loadWeek(); }

  prevWeek() { this.weekStart.update(d => subWeeks(d, 1)); this.loadWeek(); }
  nextWeek() { this.weekStart.update(d => addWeeks(d, 1)); this.loadWeek(); }

  toggleDay(date: string) {
    this.expandedDay.update(d => d === date ? null : date);
  }

  dayBadgeClass(day: DaySummary): string {
    if (day.isHoliday)          return 'badge-holiday';
    if (day.isMissed)           return 'badge-missed';
    if (!day.dayStatus)         return 'badge-empty';
    return {
      APPROVED:                 'badge-approved',
      PENDING:                  'badge-pending',
      REJECTED:                 'badge-rejected',
      CLARIFICATION_REQUESTED:  'badge-clarification'
    }[day.dayStatus] ?? 'badge-empty';
  }

  private loadWeek() {
    this.loading.set(true);
    this.timesheetService.getWeek(formatDate(this.weekStart()))
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(data => this.weekData.set(data));
  }
}
UX Rules
Day columns are always 7 (Mon–Sun); weekends have muted background and "Weekend" label
Holiday days: light teal background, 🏖️ icon, holiday name on hover, "Log Time" button (no missed-date CTA)
Missed weekdays: amber background, "Log Now" CTA linking to Log Time for that date
Current day: highlighted border (today indicator)
Day_Status badge colours: green=APPROVED, amber=PENDING, red=REJECTED, blue=CLARIFICATION_REQUESTED, grey=no entries
Clicking a day column expands it inline to show all task entries with individual statuses
Mixed-status days show each task's status chip; REJECTED tasks show the rejection reason on hover
Auto-approved tasks show "Auto-Approved" chip instead of a status badge
Future weeks beyond current week are read-only (no add buttons)
3. Employee Dashboard Component
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ My Dashboard                                                        │
├──────────────┬──────────────┬──────────────┬────────────────────────┤
│ Total Hours  │   Pending    │   Approved   │    Missed Dates        │
│  This Week   │   Entries    │   Entries    │                        │
│   32.5 hrs   │      3       │     12       │        1               │
│  ████████░   │   🟡 3       │   ✅ 12      │   ⚠ Mon Apr 7          │
└──────────────┴──────────────┴──────────────┴────────────────────────┘
│                                                                     │
│  ┌── This Month Status ──────────┐  ┌── Missed Dates ────────────┐ │
│  │   Donut Chart                 │  │  ⚠ Mon Apr 7  [Log Now →]  │ │
│  │   ✅ Approved  65%            │  │                            │ │
│  │   🟡 Pending   25%            │  └────────────────────────────┘ │
│  │   🔴 Rejected  10%            │                                 │
│  └───────────────────────────────┘                                 │
│                                                                     │
│  ┌── Recent Activity ────────────────────────────────────────────┐ │
│  │  ✅ "Backend API" approved by John M.          2 hours ago    │ │
│  │  🟡 "Sprint planning" submitted                4 hours ago    │ │
│  │  🔴 "DB review" rejected — "Wrong project"     Yesterday      │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/timesheets/dashboard
// Response 200 OK
{
  "weeklyTotal": 32.5,
  "pendingCount": 3,
  "approvedCount": 12,
  "missedDates": ["2025-04-07"],
  "monthlyStatusBreakdown": {
    "APPROVED": 65, "PENDING": 25, "REJECTED": 10
  },
  "recentActivity": [
    {
      "type": "APPROVED",
      "taskName": "Backend API",
      "actorName": "John Manager",
      "timestamp": "2025-04-14T06:30:00Z",
      "entryId": 101
    }
  ]
}
UX Rules
KPI cards load with skeleton screens; data populates within 2 seconds
Missed dates list each show a "Log Now" link that navigates to Log Time pre-filled with that date
Recent activity timestamps show relative time ("2 hours ago"); absolute time on hover
Donut chart is interactive — clicking a segment filters the history view to that status
4. Team Review Component (Manager)
Page Layout
┌─────────────────────────────────────────────────────────────────────────────────┐
│ My Team > John Doe > Week of Apr 14, 2025          [← Prev Week] [Next Week →] │
│                                          [Approve All Pending] [Reject All]     │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────────┤
│  MON 14  │  TUE 15  │  WED 16  │  THU 17  │  FRI 18  │  SAT 19  │    SUN 20   │
│  8.0 hrs │  7.5 hrs │  0 hrs   │ 9.5 hrs⚠️│  0 hrs   │  3.0 hrs │   0 hrs     │
│ ●APPROVED│ ●PENDING │  MISSED  │ ●PENDING │ 🏖️Holiday│ ●PENDING │   (empty)   │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────────┤
│ ▼ TUE 15 — 7.5 hrs  [PENDING]                [✅ Approve Day] [❌ Reject Day]   │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │ ▌ Task: "API endpoints"   Project: TMS   4.0 hrs  🟡 PENDING           │   │
│   │   [✅ Approve] [❌ Reject] [💬 Clarify]                                  │   │
│   │ ▌ Task: "Code review"     Project: TMS   3.5 hrs  🟡 PENDING           │   │
│   │   [✅ Approve] [❌ Reject] [💬 Clarify]                                  │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│ ▼ THU 17 — 9.5 hrs  ⚠️ OVERTIME  [PENDING]                                     │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │ ⚠ Overtime Justification: "Client deadline — emergency release"         │   │
│   │ ▌ Task: "Emergency fix"   Project: ClientX  5.0 hrs  🟡 PENDING        │   │
│   │ ▌ Task: "Deployment"      Project: ClientX  4.5 hrs  🟡 PENDING        │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/approvals/team/{employeeId}/week?weekStart=2025-04-14
// Response 200 OK
{
  "employee": { "id": 5, "fullName": "John Doe", "department": "Engineering" },
  "weekStart": "2025-04-14",
  "days": [
    {
      "date": "2025-04-15", "dayOfWeek": "TUESDAY",
      "totalHours": 7.5, "dayStatus": "PENDING",
      "hasOvertime": false,
      "entries": [
        {
          "id": 110, "taskName": "API endpoints", "projectName": "TMS",
          "hours": 4.0, "status": "PENDING",
          "isAutoApproved": false, "taskDescription": "Implemented approval endpoints"
        }
      ]
    },
    {
      "date": "2025-04-17", "dayOfWeek": "THURSDAY",
      "totalHours": 9.5, "dayStatus": "PENDING",
      "hasOvertime": true,
      "overtimeJustification": "Client deadline — emergency release",
      "entries": [...]
    }
  ]
}

// POST /api/approvals/entries/{id}/approve
// Request: (no body)
// Response 200 OK
{ "entryId": 110, "newStatus": "APPROVED", "dayStatus": "PENDING" }

// POST /api/approvals/entries/{id}/reject
// Request
{ "reason": "Wrong project assigned — should be ClientX not TMS" }
// Response 200 OK
{ "entryId": 110, "newStatus": "REJECTED", "dayStatus": "REJECTED" }

// POST /api/approvals/entries/{id}/clarify
// Request
{ "message": "Can you clarify which sprint this belongs to?" }
// Response 200 OK
{ "entryId": 110, "newStatus": "CLARIFICATION_REQUESTED", "dayStatus": "CLARIFICATION_REQUESTED" }

// POST /api/approvals/day/{employeeId}/{date}/approve  (bulk day approve)
// Response 200 OK
{ "date": "2025-04-15", "approvedCount": 2, "dayStatus": "APPROVED" }
Component Code
// features/manager/team-review/team-review.component.ts
@Component({ selector: 'tms-team-review', standalone: true })
export class TeamReviewComponent {
  private approvalService = inject(ApprovalService);
  private route           = inject(ActivatedRoute);

  employeeId  = signal(+this.route.snapshot.params['employeeId']);
  weekStart   = signal(startOfWeek(new Date(), { weekStartsOn: 1 }));
  weekData    = signal<TeamWeekResponse | null>(null);
  expandedDay = signal<string | null>(null);
  loading     = signal(false);

  approveEntry(entryId: number) {
    this.approvalService.approve(entryId).subscribe(res => this.updateEntryStatus(res));
  }

  rejectEntry(entryId: number, reason: string) {
    this.approvalService.reject(entryId, reason).subscribe(res => this.updateEntryStatus(res));
  }

  requestClarification(entryId: number, message: string) {
    this.approvalService.clarify(entryId, message).subscribe(res => this.updateEntryStatus(res));
  }

  approveDay(date: string) {
    this.approvalService.approveDay(this.employeeId(), date)
      .subscribe(res => this.updateDayStatus(date, res));
  }
}
UX Rules
Overtime days show amber ⚠️ badge on the day column header; overtime justification shown in a highlighted box at the top of the expanded day
Each task row has three action buttons: ✅ Approve, ❌ Reject, 💬 Clarify — hidden for AUTO_APPROVED tasks
Reject opens an inline form requiring a reason (min 10 chars) before confirming
Clarify opens the inline clarification thread panel (see Clarification Thread component)
Day-level "Approve Day" / "Reject Day" buttons act on all PENDING tasks for that day
"Approve All Pending" button at the top acts on all PENDING tasks across the entire week
Auto-approved tasks show "Auto-Approved" chip — no action buttons
Day_Status badge updates in real time as individual tasks are actioned (no page reload)
5. Clarification Thread Component (Manager + Employee)
Page Layout
┌── Clarification Thread — "Database schema review" ──────────────────┐
│  Task: Database schema review  |  Project: TMS  |  2.0 hrs          │
│  Status: 🔵 CLARIFICATION_REQUESTED                                  │
├──────────────────────────────────────────────────────────────────────┤
│  👤 John Manager  [Manager]                        Apr 14, 10:30 AM  │
│  "Can you clarify which sprint this task belongs to?                 │
│   The project code doesn't match the sprint board."                  │
│                                                                      │
│  👤 Jane Employee  [Employee]                      Apr 14, 11:15 AM  │
│  "This was for Sprint 12 backlog cleanup. I'll update the            │
│   project code to match."                                            │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │ Type your reply...                                           │    │
│  └──────────────────────────────────────────────────────────────┘    │
│  [Send Reply]                                                        │
│                                                                      │
│  ─────────────────────────────────────────────────────────────────   │
│  [✅ Approve Entry]                          [❌ Reject Entry]        │
└──────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/clarifications/entries/{entryId}
// Response 200 OK
{
  "entryId": 110,
  "taskName": "Database schema review",
  "status": "CLARIFICATION_REQUESTED",
  "messages": [
    {
      "id": 1, "authorId": 20, "authorName": "John Manager",
      "authorRole": "MANAGER", "message": "Can you clarify...",
      "createdAt": "2025-04-14T05:00:00Z"
    },
    {
      "id": 2, "authorId": 5, "authorName": "Jane Employee",
      "authorRole": "EMPLOYEE", "message": "This was for Sprint 12...",
      "createdAt": "2025-04-14T05:45:00Z"
    }
  ],
  "isReadOnly": false
}

// POST /api/clarifications/entries/{entryId}
// Request
{ "message": "Thanks, that makes sense. Approving now." }
// Response 201 Created
{
  "id": 3, "authorName": "John Manager", "authorRole": "MANAGER",
  "message": "Thanks, that makes sense. Approving now.",
  "createdAt": "2025-04-14T06:00:00Z"
}
UX Rules
Thread opens as an inline slide-in panel on the right side of the team review page (not a modal)
Messages are displayed in chronological order; newest at the bottom
Manager messages are right-aligned with a navy background; employee messages are left-aligned with a light grey background
Role badge (Manager / Employee) shown next to each author name
Thread is read-only once entry is APPROVED or REJECTED — input field and send button are hidden; a "Thread closed" label is shown
Approve/Reject buttons at the bottom of the thread allow the manager to act without leaving the thread
Employee sees the thread in read-only mode from their weekly view (no approve/reject buttons)
6. Manager Dashboard Component
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ Manager Dashboard                                                   │
├──────────────┬──────────────┬──────────────┬────────────────────────┤
│   Direct     │   Pending    │    Missed    │   Approved This Week   │
│   Reports    │  Approvals   │   Entries    │                        │
│      8       │      5       │      2       │         24             │
└──────────────┴──────────────┴──────────────┴────────────────────────┘
│                                                                     │
│  ┌── Team Summary ───────────────────────────────────────────────┐ │
│  │  Name          Hours/Wk  Pending  Missed  Last Submission     │ │
│  │  ─────────────────────────────────────────────────────────    │ │
│  │  🟡 John Doe    32.5 hrs    3       0     Apr 14, 9:00 AM  →  │ │
│  │  🟡 Jane Smith  28.0 hrs    2       0     Apr 14, 8:30 AM  →  │ │
│  │  ⚠  Bob Jones   0.0 hrs    0       1     Apr 11, 5:00 PM  →  │ │
│  │  ✅ Alice Wu    40.0 hrs    0       0     Apr 14, 10:00 AM →  │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  [Send Missing Entry Reminder to All]                               │
│                                                                     │
│  ┌── My Own Timesheet (Employee View) ───────────────────────────┐ │
│  │  [Embedded Employee Dashboard widget]                         │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/manager/dashboard
// Response 200 OK
{
  "totalDirectReports": 8,
  "pendingApprovalsCount": 5,
  "missedEntriesCount": 2,
  "approvedThisWeek": 24,
  "teamSummary": [
    {
      "employeeId": 5, "fullName": "John Doe",
      "hoursThisWeek": 32.5, "pendingCount": 3,
      "missedCount": 0, "lastSubmissionAt": "2025-04-14T03:30:00Z",
      "hasPendingItems": true
    }
  ]
}

// POST /api/manager/reminders/missing
// Request: (no body — sends to all direct reports with missed dates)
// Response 200 OK
{ "remindersSent": 2, "recipients": ["bob.jones@company.com"] }

// POST /api/manager/reminders/employee/{id}
// Request: (no body)
// Response 200 OK
{ "reminderSent": true, "recipient": "bob.jones@company.com" }
UX Rules
Team summary table sorted by: rows with pending items first, then by missed count descending
Each row has a "→" arrow that navigates to that employee's team review page
"Send Missing Entry Reminder to All" button is only shown when missedEntriesCount > 0
Individual "Send Reminder" button available on each row with missed entries
Manager's own employee dashboard is embedded as a collapsible widget at the bottom
7. HR Dashboard Component
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ HR Overview                                                         │
├──────────────┬──────────────┬──────────────┬────────────────────────┤
│   Total      │  Compliance  │  Pending     │  Avg Hours/Employee    │
│  Employees   │    Rate      │  Approvals   │     This Week          │
│     120      │    94.2%     │     18       │       36.8 hrs         │
└──────────────┴──────────────┴──────────────┴────────────────────────┘
│                                                                     │
│  ┌── Department Breakdown ────────────────────────────────────────┐ │
│  │  Department    Employees  Compliant  Missed  Compliance %      │ │
│  │  Engineering       45        43        2       95.6%           │ │
│  │  Design            12        11        1       91.7%           │ │
│  │  Marketing         18        18        0      100.0%           │ │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  [Send Missing Entry Reminder — All]  [Send Pending Approval Reminder] │
└─────────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/hr/dashboard
// Response 200 OK
{
  "totalEmployees": 120,
  "complianceRate": 94.2,
  "pendingApprovalsOrgWide": 18,
  "avgHoursPerEmployee": 36.8,
  "departmentBreakdown": [
    {
      "department": "Engineering", "employeeCount": 45,
      "compliantCount": 43, "missedCount": 2, "complianceRate": 95.6
    }
  ]
}
UX Rules
HR cannot see task descriptions — only aggregated hours per employee per day
Clicking a department row expands to show employee-level daily summaries (hours only, no task detail)
Reminder buttons show a preview modal of the email content before sending
Compliance rate is colour-coded: green ≥ 95%, amber 80–94%, red < 80%
8. Holiday Calendar Component (HR + Admin)
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ Holiday Calendar — 2025                    [+ Add Holiday] [Import] │
├─────────────────────────────────────────────────────────────────────┤
│  Filter: [All Types ▼]  [All Departments ▼]                         │
├──────────┬──────────────────────┬──────────────┬────────────────────┤
│  Date    │  Holiday Name        │  Type        │  Applicable To     │
├──────────┼──────────────────────┼──────────────┼────────────────────┤
│ Jan 26   │  Republic Day        │  Public      │  All               │
│ Apr 18   │  Good Friday         │  Public      │  All               │
│ Aug 15   │  Independence Day    │  Public      │  All               │
│ Oct 02   │  Gandhi Jayanti      │  Public      │  All               │
│ Dec 25   │  Christmas           │  Company     │  All               │
└──────────┴──────────────────────┴──────────────┴────────────────────┘
Request / Response DTOs
// POST /api/hr/holidays
// Request
{
  "name": "Republic Day",
  "holidayDate": "2025-01-26",
  "type": "PUBLIC_HOLIDAY",
  "applicableTo": "ALL"
}
// Response 201 Created
{
  "id": 1, "name": "Republic Day", "holidayDate": "2025-01-26",
  "type": "PUBLIC_HOLIDAY", "applicableTo": "ALL",
  "createdBy": "HR Admin", "createdAt": "2025-01-01T00:00:00Z"
}
UX Rules
Only HR and Admin see the Add/Edit/Delete controls; employees see the calendar read-only
Add Holiday opens a slide-in form panel (not a modal) with date picker, name, type dropdown, department multi-select
Bulk import via CSV shows a preview table before confirming; errors reported row by row
Deleting a holiday shows a confirmation dialog warning: "This may re-flag missed dates for employees"
9. Admin — User Management Component
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ User Management                    [+ Add User] [Import CSV]        │
├─────────────────────────────────────────────────────────────────────┤
│  🔍 Search by name or email...  [Role ▼] [Status ▼] [Dept ▼]       │
├──────────┬──────────────┬──────────┬──────────┬─────────────────────┤
│  Name    │  Email       │  Roles   │  Status  │  Actions            │
├──────────┼──────────────┼──────────┼──────────┼─────────────────────┤
│ John Doe │ john@co.com  │ EMP, MGR │ Active   │ [Edit][Deactivate]  │
│ Jane S.  │ jane@co.com  │ EMP      │ Active   │ [Edit][Deactivate]  │
│ Bob J.   │ bob@co.com   │ EMP      │ Inactive │ [Edit][Reactivate]  │
└──────────┴──────────────┴──────────┴──────────┴─────────────────────┘
Request / Response DTOs
// POST /api/admin/users
// Request
{
  "fullName": "Alice Wu", "email": "alice@company.com",
  "employeeId": "EMP-042", "department": "Engineering",
  "roles": ["EMPLOYEE"], "managerId": 20, "timezone": "Asia/Kolkata"
}
// Response 201 Created
{
  "id": 55, "fullName": "Alice Wu", "email": "alice@company.com",
  "status": "ACTIVE", "forcePasswordChange": true,
  "message": "User created. Welcome email sent."
}

// POST /api/admin/users/{id}/deactivate
// Response 200 OK
{ "id": 55, "status": "INACTIVE" }
UX Rules
Search is instant (debounced 300ms); filters stack
Deactivate shows a confirmation dialog: "This user will lose access immediately. Their historical data is preserved."
Edit opens a slide-in form panel pre-filled with current values
Bulk CSV import shows a preview table; errors shown row by row with the specific field that failed
10. Admin — Audit Log Component
Page Layout
┌─────────────────────────────────────────────────────────────────────┐
│ Audit Log                                          [Export CSV]     │
├─────────────────────────────────────────────────────────────────────┤
│  Date Range: [Apr 1 – Apr 14]  Actor: [All ▼]  Action: [All ▼]     │
├──────────────┬──────────────┬──────────────┬────────────────────────┤
│  Timestamp   │  Actor       │  Action      │  Entity                │
├──────────────┼──────────────┼──────────────┼────────────────────────┤
│ Apr 14 09:00 │ John Manager │ APPROVED     │ Entry #101             │
│ Apr 14 08:30 │ Jane Employee│ SUBMITTED    │ Entry #101             │
│ Apr 14 08:00 │ SYSTEM       │ AUTO_APPROVED│ Entry #99              │
│ Apr 13 17:00 │ SYSTEM       │ REMINDER_SENT│ User #5                │
└──────────────┴──────────────┴──────────────┴────────────────────────┘
│ ▼ Apr 14 09:00 — APPROVED by John Manager                           │
│   Before: { status: "PENDING" }                                     │
│   After:  { status: "APPROVED" }                                    │
└─────────────────────────────────────────────────────────────────────┘
UX Rules
Audit log is read-only — no edit or delete controls anywhere on the page
Clicking a row expands it to show the before/after JSON snapshot
Export generates a CSV asynchronously for large date ranges; user notified when ready
Default date range is last 7 days; can be extended to full 2-year retention window
11. Notification Panel Component
Page Layout
┌── Notifications ─────────────────────────────────────────────────┐
│  [Mark all as read]                                               │
├───────────────────────────────────────────────────────────────────┤
│  🔵 ● "Backend API" approved by John M.          2 hours ago  →  │
│  🟡 ● "Sprint planning" submitted                4 hours ago  →  │
│  🔴 ● "DB review" rejected — "Wrong project"     Yesterday    →  │
│  💬   "Clarification requested on Task #110"     2 days ago   →  │
│  ─────────────────────────────────────────────────────────────    │
│  ✅   "Code review" approved                     3 days ago   →  │
└───────────────────────────────────────────────────────────────────┘
Request / Response DTOs
// GET /api/notifications
// Response 200 OK
{
  "unreadCount": 3,
  "notifications": [
    {
      "id": 201, "type": "APPROVED", "isRead": false,
      "message": "\"Backend API\" approved by John Manager",
      "deepLink": "/timesheets/weekly?date=2025-04-14",
      "createdAt": "2025-04-14T06:30:00Z"
    }
  ]
}

// POST /api/notifications/{id}/read
// Response 200 OK
{ "id": 201, "isRead": true }

// POST /api/notifications/read-all
// Response 200 OK
{ "markedRead": 3 }
Component Code
// shared/components/notification-panel/notification-panel.component.ts
@Component({ selector: 'tms-notification-panel', standalone: true })
export class NotificationPanelComponent implements OnInit, OnDestroy {
  private notificationService = inject(NotificationService);
  private store               = inject(Store);

  isOpen        = signal(false);
  notifications = this.store.selectSignal(selectNotifications);
  unreadCount   = this.store.selectSignal(selectUnreadCount);

  ngOnInit() {
    // Load initial notifications via REST
    this.notificationService.loadNotifications().subscribe(data =>
      this.store.dispatch(NotificationActions.loaded({ notifications: data.notifications }))
    );
    // Subscribe to real-time WebSocket updates
    this.notificationService.connectWebSocket().subscribe(notification =>
      this.store.dispatch(NotificationActions.received({ notification }))
    );
  }

  markRead(id: number) {
    this.notificationService.markRead(id).subscribe(() =>
      this.store.dispatch(NotificationActions.markRead({ id }))
    );
  }

  markAllRead() {
    this.notificationService.markAllRead().subscribe(() =>
      this.store.dispatch(NotificationActions.markAllRead())
    );
  }
}
UX Rules
Bell icon in top header shows red badge with unread count; badge disappears when count = 0
Panel opens as a slide-in overlay from the top-right; clicking outside closes it
Unread notifications have a filled dot indicator; read ones are dimmed
Each notification is a clickable row that navigates to the deep-link and marks it as read
"Mark all as read" clears all dots at once
Real-time delivery via WebSocket (STOMP /user/queue/notifications); no page refresh needed
Unread count persists across sessions until explicitly read
Timezone Detection
// timezone.service.ts
@Injectable({ providedIn: 'root' })
export class TimezoneService {
  detectTimezone(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone; // IANA timezone ID
  }

  // Called on login; stored in auth state and sent as X-User-Timezone header
  initializeTimezone(userStoredTimezone: string | null): string {
    const detected = this.detectTimezone();
    return userStoredTimezone ?? detected;
  }
}
The detected/stored timezone is sent as X-User-Timezone header on all timesheet write requests. The profile page shows the current timezone and allows manual override (saved to the user's profile via PUT /api/users/me/timezone).

Correctness Properties
A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

Property Reflection
Before writing properties, redundancy analysis was performed:

"Entry editing resets to PENDING" (2.2 AC3) and "PENDING/CLARIFICATION entries can be edited" (2.2 AC1) can be combined: the edit permission check and the status reset are two aspects of the same operation.
"Timesheet entries immutable once APPROVED" (9.2 DR2) and "Once approved, entry cannot be rejected without Admin" (3.3 AC5) both test the immutability of APPROVED entries — combined into one property.
"Manager cannot approve own entries" (3.3 AC3) and "self-approval prevention" are the same property.
"Each employee has exactly one active manager" (5.3 AC1) and "circular assignment prevention" (5.3 AC4) are related but distinct invariants — kept separate.
"Holiday dates never flagged as missed" (4.4 AC4) and "Only HR/Admin can write holidays" (4.4 AC1) are distinct properties — kept separate.
After reflection, 14 unique properties remain.

Property 1: JWT Invalidation After Logout
For any valid JWT issued to a user, after that user logs out, any subsequent authenticated request using that same JWT must be rejected with HTTP 401.

Validates: Requirements 1.1 AC6

Property 2: Employee Role Inheritance
For any user account in the system regardless of what additional roles they hold, their role set always contains EMPLOYEE.

Validates: Requirements 1.2 AC1

Property 3: Password Reset Token Single-Use
For any valid password reset token, using it once to reset a password succeeds and marks the token as used; any subsequent attempt to use the same token fails with an error.

Validates: Requirements 1.3 AC2

Property 4: Password Complexity Validation
For any candidate password string, the password validator accepts it if and only if it satisfies all of: length >= 8, contains at least one uppercase letter, contains at least one digit, and contains at least one special character. Strings failing any single criterion are rejected.

Validates: Requirements 1.3 AC3

Property 5: Future Date Submission Rejection
For any timesheet entry submission where the entry_date is after today (in the submitting user's local timezone), the API rejects the submission with a validation error.

Validates: Requirements 2.1 AC1

Property 6: Overtime Justification Enforcement
For any set of task entries submitted for a single day where the sum of hours exceeds the configured overtime threshold (default 9), the submission is rejected if no overtime_justification is provided or if the justification is fewer than 10 characters. The submission succeeds when a valid justification (10–300 characters) is provided.

Validates: Requirements 2.1 AC4, 9.2 DR3

Property 7: Auto-Approval for Sub-Hour Tasks
For any task entry submitted with hours = 0.5 (the only value < 1 given the 0.5-increment constraint), the resulting entry status is AUTO_APPROVED immediately upon submission, no manager notification is generated, and the entry is included in Day_Status computation as APPROVED.

Validates: Requirements 2.1 AC5, 3.3 AC6

Property 8: Past-Entry Edit Window Enforcement
For any timesheet entry submission where the entry_date is more than the configured edit window (default 30 days) before today, the submission is rejected unless the actor has the ADMIN role.

Validates: Requirements 2.1 AC8

Property 9: Entry Edit Permission and Status Reset
For any timesheet entry, editing is permitted if and only if the entry's current status is PENDING or CLARIFICATION_REQUESTED. Upon a successful edit, the entry's status is reset to PENDING regardless of its previous status, and the assigned manager receives a notification.

Validates: Requirements 2.2 AC1, 2.2 AC3

Property 10: Day_Status Precedence Computation
For any non-empty collection of task entries for a given employee on a given date, the computed Day_Status satisfies the following precedence rules: - If any entry has status REJECTED → Day_Status = REJECTED - Else if any entry has status CLARIFICATION_REQUESTED → Day_Status = CLARIFICATION_REQUESTED - Else if all entries have status APPROVED or AUTO_APPROVED → Day_Status = APPROVED - Otherwise → Day_Status = PENDING

Validates: Requirements 2.4 AC6, 3.3 AC6

Property 11: APPROVED Entry Immutability
For any timesheet entry in APPROVED status, any attempt to modify or delete it by an actor without the ADMIN role is rejected. The entry's status and content remain unchanged after the rejected attempt.

Validates: Requirements 3.3 AC5, 9.2 DR2

Property 12: Self-Approval Prevention
For any timesheet entry, if the actor attempting to approve or reject the entry is the same user as the entry's owner (user_id), the approval action is blocked and the entry is re-routed to the owner's skip-level manager (or any HR user if no skip-level manager exists).

Validates: Requirements 3.3 AC3

Property 13: Holiday Calendar Write Access Control
For any user whose role set does not include HR or ADMIN, any attempt to create, update, or delete a holiday calendar entry is rejected with HTTP 403.

Validates: Requirements 4.4 AC1

Property 14: Holiday Exclusion from Missed-Date Detection
For any date that exists in the holiday_calendar table with applicable_to covering a given employee, the missed-date detection job does not flag that employee as having a missed date on that date, regardless of whether the employee logged time on that date.

Validates: Requirements 4.4 AC4, 2.4 AC4

Property 15: Circular Manager Assignment Prevention
For any proposed manager assignment (employee_id → manager_id), if accepting the assignment would create a cycle in the manager-employee graph (i.e., following the chain of manager assignments from manager_id eventually reaches employee_id), the assignment is rejected.

Validates: Requirements 5.3 AC4

Property 16: UTC Timezone Round-Trip
For any UTC timestamp and any valid IANA timezone identifier, converting the UTC timestamp to the local timezone and then converting back to UTC produces a value equal to the original UTC timestamp (within the same calendar date boundary).

Validates: Requirements 7.3 AC1

Error Handling
Backend Error Handling
All exceptions are handled by a global @RestControllerAdvice (GlobalExceptionHandler) that maps exceptions to consistent HTTP responses:

Exception	HTTP Status	Response Body
ValidationException	400 Bad Request	{field, message} array
EntityNotFoundException	404 Not Found	{error, message}
AccessDeniedException	403 Forbidden	{error: "Access denied"}
SelfApprovalException	409 Conflict	{error, message, reroutedTo}
OvertimeJustificationRequiredException	400 Bad Request	{error, message, dailyTotal}
EntryNotEditableException	409 Conflict	{error, currentStatus}
CircularAssignmentException	409 Conflict	{error, cycle}
AccountLockedException	423 Locked	{error, lockedUntil}
TokenExpiredException	401 Unauthorized	{error: "Token expired"}
MethodArgumentNotValidException	400 Bad Request	Field-level validation errors
Exception (catch-all)	500 Internal Server Error	{error: "An unexpected error occurred"} — no stack trace in response
Sensitive information policy: Stack traces, SQL errors, and internal class names are never included in API error responses. They are logged server-side with a correlation ID that can be referenced in support tickets.

Email Delivery Failures
Email sending is wrapped in a retry mechanism:

@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
public void sendEmail(EmailMessage message) { ... }

@Recover
public void emailRecovery(Exception e, EmailMessage message) {
    // Log failure with full details
    // Store in failed_email_log table for manual retry
}
Async Export Failures
Export jobs that fail update their status to FAILED with an error message. The user receives an in-app notification that the export failed with a retry option.

WebSocket Disconnection
If a WebSocket connection drops, the Angular client automatically reconnects with exponential backoff (1s, 2s, 4s, max 30s). Notifications missed during disconnection are fetched via the REST /api/notifications endpoint on reconnect.

Frontend Error Handling
API errors: displayed as user-friendly messages (not raw error codes) with a retry button.
Network offline: persistent banner "You're offline. Changes will sync when reconnected."
Session expiry (401): modal prompting re-login without losing current page state (Angular route state preserved).
Long operations (>500ms): progress indicator shown.
Testing Strategy
Overview
The testing strategy uses a dual approach: property-based tests for universal correctness properties and example-based unit/integration tests for specific behaviors.

Property-Based Testing
Library: jqwik for Java (Spring Boot backend). Each property test runs a minimum of 100 iterations with randomly generated inputs.

Each property test is tagged with a comment referencing the design property:

// Feature: timesheet-management-system, Property 10: Day_Status Precedence Computation
@Property(tries = 200)
void dayStatusPrecedenceComputation(@ForAll List<@From("entryStatuses") EntryStatus> statuses) {
    // Generate random combinations of task statuses
    // Verify DayStatusComputer.compute() returns correct Day_Status
}
Properties to implement as property-based tests:

Property	Test Class	Key Generators
P1: JWT Invalidation After Logout	AuthPropertyTest	Random valid JWTs
P3: Password Reset Token Single-Use	PasswordResetPropertyTest	Random token strings
P4: Password Complexity Validation	PasswordValidationPropertyTest	Random strings (valid + invalid)
P5: Future Date Submission Rejection	TimesheetSubmissionPropertyTest	Random future dates
P6: Overtime Justification Enforcement	OvertimeValidationPropertyTest	Random hour combinations summing > 9
P7: Auto-Approval for Sub-Hour Tasks	AutoApprovalPropertyTest	Fixed 0.5 hr entries with random task data
P8: Past-Entry Edit Window	EditWindowPropertyTest	Random dates > 30 days in past
P9: Entry Edit Permission and Status Reset	EntryEditPropertyTest	Random entry statuses
P10: Day_Status Precedence	DayStatusPropertyTest	Random status combinations
P11: APPROVED Entry Immutability	EntryImmutabilityPropertyTest	Random non-Admin actors
P12: Self-Approval Prevention	SelfApprovalPropertyTest	Random user/entry combinations
P13: Holiday Calendar Access Control	HolidayAccessPropertyTest	Random non-HR/Admin users
P14: Holiday Exclusion from Missed-Date	HolidayMissedDatePropertyTest	Random holiday dates + user timezones
P15: Circular Assignment Prevention	ManagerAssignmentPropertyTest	Random manager graphs
P16: UTC Timezone Round-Trip	TimezonePropertyTest	Random UTC timestamps + IANA timezones
Unit Tests
Unit tests cover specific examples, edge cases, and integration points. Target: 80% code coverage.

Key unit test areas:

DayStatusComputer: All 4 status combinations, empty list, single entry, all-auto-approved
OvertimeValidator: Boundary values (8.0, 8.5, 9.0, 9.5), with/without justification
TimezoneUtil: DST transitions, UTC+0, UTC+5:30, UTC-5, date boundary cases
MissedDateDetectionJob: Weekend skip, holiday skip, already-logged skip
ManagerAssignmentService: Direct circular (A→B, B→A), indirect circular (A→B→C→A)
ApprovalService: Self-approval routing, skip-level routing, HR fallback
AuthService: Account lockout counter, token blacklist, password reset token expiry
TimesheetService: Auto-approval threshold, overtime justification validation, 30-day window
Integration Tests
Integration tests use @SpringBootTest with an in-memory H2 database (or Testcontainers with MS SQL Server for full fidelity):

Full submission → approval workflow
Clarification thread lifecycle
Manager reassignment mid-week (Scenario 13/15)
Holiday retroactive addition (Scenario 12)
Multi-timezone missed-date detection
Frontend Tests
Unit tests: Jest + Angular Testing Library for component logic
E2E tests: Playwright for critical user flows (login, log time, weekly view, approval)
Accessibility: axe-core integration in Playwright tests for WCAG 2.1 AA checks
Performance Tests
Load test with k6: 500 concurrent users, read-heavy workload (weekly view, dashboard)
Target: 95th percentile < 500ms for reads, < 1s for writes
Async export: verify jobs complete within 60 seconds for 10,000-row datasets
Non-Functional Design Decisions
Performance
Indexing strategy (see Data Models section for full index definitions): - timesheet_entries(user_id, entry_date) — primary query pattern for employee weekly view - timesheet_entries(assigned_manager_id, entry_date) — manager team review - timesheet_entries(user_id, entry_date, status) — composite for Day_Status computation - notifications(user_id, is_read) — unread count badge query - audit_logs(created_at) — time-range queries on audit log

Query optimization: - Day_Status is computed in-memory from a single indexed query (no subquery per task). The service fetches all entries for a day in one query and passes them to DayStatusComputer. - Dashboard KPI queries use aggregation at the DB level (COUNT, SUM with GROUP BY) rather than loading all rows into memory. - The manager team dashboard uses a single query joining timesheet_entries with manager_assignments filtered to the current week.

Async exports: - Reports exceeding 1,000 rows are generated asynchronously via @Async with a dedicated ThreadPoolTaskExecutor (core pool: 2, max pool: 5, queue capacity: 50). - Export files are written to a configurable temp directory; the download URL is returned via WebSocket push or polling. - Files are cleaned up after 24 hours via a scheduled cleanup job.

Caching: - System configuration values are cached in-memory (Spring @Cacheable) with a 5-minute TTL. Cache is evicted immediately on Admin config update. - Holiday calendar for the current year is cached with a 1-hour TTL. - Project list (active projects per user) is cached per user with a 10-minute TTL.

Security
All inputs validated via Bean Validation (@Valid) at the controller layer; additional business validation in the service layer.
SQL injection: Spring Data JPA uses parameterized queries exclusively. No @Query with string concatenation.
XSS: All user-provided text fields are stored as-is but escaped on output. Angular's template binding ({{ }}) auto-escapes HTML. Rich text is not supported.
Sensitive fields (email, employee_id) are masked in application logs using a custom Logback converter.
Audit log DB user has INSERT-only permission on audit_logs — no UPDATE or DELETE.
Password reset tokens are stored as bcrypt hashes (not plaintext) in the DB.
Scalability
Stateless backend: No server-side session state. JWT carries all auth context. Multiple instances can be deployed behind a load balancer.
WebSocket scaling: For multi-instance deployments, a message broker (e.g., RabbitMQ or Redis pub/sub) is used as the STOMP message broker backend so WebSocket messages are delivered regardless of which instance the client is connected to. For v1.0 single-instance deployment, the in-memory STOMP broker is sufficient.
Scheduler deduplication: The missed-date detection scheduler uses a distributed lock (via a scheduler_locks DB table with optimistic locking) to prevent duplicate execution when multiple instances are running.
Multi-tenancy path: The schema is designed with organization_id as a future addition. All tables can have an organization_id column added via Flyway migration without requiring a full redesign (NFR 8.4 NFR2).
Maintainability
OpenAPI 3.0 documentation generated via springdoc-openapi — available at /swagger-ui.html in non-production environments.
All API endpoints annotated with @Operation and @ApiResponse for complete documentation.
Flyway migrations are versioned and checksummed — no manual schema changes permitted.
Backend achieves >= 80% unit test coverage enforced via JaCoCo in the CI pipeline.
Frontend feature modules are lazy-loaded; no circular dependencies enforced via ESLint rules.
Repository & Deployment Structure
Mono-Repo Layout
The TMS uses a mono-repo with two independent sub-projects and path-based CI triggers. This gives atomic cross-cutting commits while keeping Docker builds isolated — a frontend CSS change never triggers a Java rebuild.

tms/                                    ← single Git repository
├── backend/                            ← Spring Boot / Java 21
│   ├── src/
│   │   ├── main/java/com/tns/tms/
│   │   └── main/resources/
│   │       └── db/migration/           ← Flyway SQL migrations
│   ├── pom.xml
│   ├── Dockerfile                      ← Backend image (JRE 21 Alpine)
│   └── .dockerignore
│
├── frontend/                           ← Angular 21
│   ├── src/app/
│   ├── nginx.conf                      ← Nginx config (proxy + routing)
│   ├── package.json
│   ├── angular.json
│   ├── Dockerfile                      ← Frontend image (Nginx Alpine)
│   └── .dockerignore
│
├── docker-compose.yml                  ← Local dev: wires all 3 services
├── docker-compose.prod.yml             ← Production overrides
├── .env.example                        ← Environment variable template
├── .github/
│   └── workflows/
│       ├── backend-ci.yml              ← Triggers only on backend/** changes
│       ├── frontend-ci.yml             ← Triggers only on frontend/** changes
│       └── pr-checks.yml               ← Runs on every PR (lint + test)
└── README.md
Why Mono-Repo
Factor	Mono-Repo	Two Repos
Cross-cutting feature (new API + Angular service)	1 PR, 1 review, 1 merge	2 PRs, coordination required
Docker build cost	Path-filtered — only changed side rebuilds	Always independent, but no shared context
OpenAPI contract enforcement	Auto-generate Angular client from backend spec in same pipeline	Requires external contract registry
Local dev setup	git clone + docker compose up	Clone two repos, configure separately
Independent scaling	✅ Separate Dockerfiles, separate containers	✅ Same
Access control	Coarser (one repo)	Finer (per-repo permissions)
Long-term maintenance	Lower friction for a single product team	Better for fully independent teams
Decision: Mono-repo with path-based CI. The TMS is a single product built by one team — atomic commits and shared tooling outweigh the access control benefit of two repos.

CI/CD Pipeline Design
Overview
graph LR
    subgraph Triggers
        PR[Pull Request]
        PushMain[Push to main]
        PushBackend[Push — backend/**]
        PushFrontend[Push — frontend/**]
    end

    subgraph PR Checks
        Lint[Lint + Format Check]
        UnitTest[Unit Tests]
        SecurityScan[OWASP Dependency Check]
    end

    subgraph Backend Pipeline
        BackendBuild[Maven Build + Test]
        BackendCoverage[JaCoCo Coverage ≥ 80%]
        BackendImage[Build Docker Image]
        BackendPush[Push to Registry]
        BackendDeploy[Deploy to Environment]
    end

    subgraph Frontend Pipeline
        FrontendBuild[npm ci + ng build --prod]
        FrontendTest[Jest Unit Tests]
        FrontendE2E[Playwright E2E]
        FrontendImage[Build Docker Image]
        FrontendPush[Push to Registry]
        FrontendDeploy[Deploy to Environment]
    end

    PR --> Lint
    PR --> UnitTest
    PR --> SecurityScan

    PushBackend --> BackendBuild
    BackendBuild --> BackendCoverage
    BackendCoverage --> BackendImage
    BackendImage --> BackendPush
    BackendPush --> BackendDeploy

    PushFrontend --> FrontendBuild
    FrontendBuild --> FrontendTest
    FrontendTest --> FrontendE2E
    FrontendE2E --> FrontendImage
    FrontendImage --> FrontendPush
    FrontendPush --> FrontendDeploy
Backend CI Pipeline
File: .github/workflows/backend-ci.yml

name: Backend CI

on:
  push:
    branches: [main, develop]
    paths:
      - 'backend/**'          # Only triggers when backend code changes
      - '.github/workflows/backend-ci.yml'
  pull_request:
    paths:
      - 'backend/**'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/tms-backend

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven              # Cache ~/.m2 between runs — saves ~2 min per build

      - name: Build and run tests
        run: ./mvnw verify -B
        # 'verify' runs: compile → test → integration-test → verify (JaCoCo)

      - name: Check test coverage (≥ 80%)
        run: ./mvnw jacoco:check -B
        # Fails the build if coverage drops below 80%

      - name: OWASP Dependency Check
        run: ./mvnw org.owasp:dependency-check-maven:check -B
        # Fails on CVSS score ≥ 7 (High/Critical vulnerabilities)

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-results
          path: backend/target/surefire-reports/

      - name: Upload coverage report
        uses: actions/upload-artifact@v4
        with:
          name: backend-coverage
          path: backend/target/site/jacoco/

  build-and-push-image:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'

    steps:
      - uses: actions/checkout@v4

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata (tags, labels)
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=sha,prefix=sha-
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha          # GitHub Actions cache for Docker layers
          cache-to: type=gha,mode=max   # Saves all layers — max cache efficiency
Frontend CI Pipeline
File: .github/workflows/frontend-ci.yml

name: Frontend CI

on:
  push:
    branches: [main, develop]
    paths:
      - 'frontend/**'         # Only triggers when frontend code changes
      - '.github/workflows/frontend-ci.yml'
  pull_request:
    paths:
      - 'frontend/**'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/tms-frontend

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js 22
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json  # Cache node_modules

      - name: Install dependencies
        run: npm ci --prefer-offline

      - name: Lint
        run: npm run lint

      - name: Unit tests (Jest)
        run: npm run test -- --coverage --watchAll=false

      - name: Build production bundle
        run: npm run build -- --configuration production

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: frontend/dist/
          retention-days: 1

  e2e-tests:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'

    steps:
      - uses: actions/checkout@v4

      - name: Install Playwright browsers
        run: npx playwright install --with-deps chromium
        working-directory: frontend

      - name: Run Playwright E2E tests
        run: npx playwright test
        working-directory: frontend
        env:
          BASE_URL: http://localhost:4200

      - name: Upload Playwright report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: frontend/playwright-report/

  build-and-push-image:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'

    steps:
      - uses: actions/checkout@v4

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=sha,prefix=sha-
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
PR Checks Pipeline
Runs on every pull request regardless of which files changed — ensures baseline quality before any merge.

File: .github/workflows/pr-checks.yml

name: PR Checks

on:
  pull_request:
    branches: [main, develop]

jobs:
  backend-lint:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: maven }
      - name: Checkstyle
        run: ./mvnw checkstyle:check -B

  frontend-lint:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22', cache: 'npm', cache-dependency-path: frontend/package-lock.json }
      - run: npm ci --prefer-offline
      - run: npm run lint
      - run: npm run format:check   # Prettier check

  openapi-contract-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Check OpenAPI spec is up to date
        run: |
          # Build backend, generate openapi.json, compare with committed spec
          # Fails if the committed spec is out of sync with the code
          cd backend && ./mvnw spring-boot:run &
          sleep 20
          curl http://localhost:8080/v3/api-docs -o /tmp/current-spec.json
          diff /tmp/current-spec.json openapi.json || (echo "OpenAPI spec is out of date. Run: ./mvnw spring-boot:run and update openapi.json" && exit 1)
Docker Build Cost Analysis
With path-based triggers, Docker builds only happen when relevant code changes:

Change Type	Backend CI	Frontend CI	Docker Builds
Fix a bug in TimesheetService.java	✅ Runs	❌ Skipped	Backend image only
Update Angular component CSS	❌ Skipped	✅ Runs	Frontend image only
Update docker-compose.yml	❌ Skipped	❌ Skipped	Neither (no code change)
Add new API endpoint + Angular service	✅ Runs	✅ Runs	Both (correct — atomic change)
Update README.md	❌ Skipped	❌ Skipped	Neither
Estimated CI cost savings: ~60% reduction in build minutes compared to rebuilding both sides on every push. On GitHub Actions free tier (2,000 min/month), this keeps a small team well within limits.

Docker Image Tagging Strategy
Branch	Tag	Purpose
main	latest, sha-<commit>	Production deployments
develop	develop, sha-<commit>	Staging/QA deployments
Pull Request	Not pushed	Tests only, no image
Images are stored in GitHub Container Registry (GHCR) — free for public repos, included in GitHub plans for private repos. No separate Docker Hub account needed.

Local Development with Docker Compose
# First time setup
cp .env.example .env
# Edit .env with your DB password, mail credentials, JWT keys

# Start all services (DB + Backend + Frontend)
docker compose up -d

# View logs
docker compose logs -f tms-backend
docker compose logs -f tms-frontend

# Rebuild only the backend after code changes
docker compose up -d --build tms-backend

# Rebuild only the frontend after code changes
docker compose up -d --build tms-frontend

# Stop everything
docker compose down

# Stop and remove volumes (wipes the database)
docker compose down -v
Service URLs (local): - Frontend: http://localhost - Backend API: http://localhost:8080/api - Swagger UI: http://localhost:8080/swagger-ui.html - SQL Server: localhost:1433

Construction Phases
This section defines the recommended build order for the TMS, taking into account technical dependencies between backend, frontend, database, and infrastructure layers. Each phase must be fully complete and verified before the next begins.

Dependency Map
Before phases, here is the hard dependency chain across all flows:

Database Schema (Flyway)
    └── Backend Foundation (Security, Auth, User)
            ├── Employee Timesheet Flow
            │       └── Manager Approval Flow
            │               └── Clarification Thread
            ├── Notification Infrastructure (Email + WebSocket)
            │       └── All flows that send notifications
            ├── Scheduler (Missed-date detection, Reminders)
            │       └── Holiday Calendar (must exist before scheduler runs)
            └── Admin / HR / Config
                    └── Holiday Calendar
                    └── Reports & Exports

Frontend Shell (Auth + Routing + Layout)
    ├── Employee Feature Module
    │       └── Manager Feature Module (depends on employee data)
    ├── HR Feature Module
    ├── Admin Feature Module
    └── Notification Panel (depends on WebSocket infrastructure)
Phase 1 — Foundation (Backend + Database)
Goal: Running Spring Boot application with database connectivity, schema, security, and authentication. No business logic yet.

Dependencies: None — this is the base everything else builds on.

#	Task	Layer	Depends On
1.1	Mono-repo scaffold: backend/, frontend/, docker-compose.yml, .env.example	Infra	—
1.2	MS SQL Server container running via Docker Compose	Infra	1.1
1.3	Spring Boot project setup: pom.xml, package structure, application.yml	Backend	1.2
1.4	Flyway configured; V1–V3 migrations: users, user_roles, projects, project_assignments	Database	1.3
1.5	User JPA entity, UserRepository, UserService (CRUD)	Backend	1.4
1.6	Spring Security config: JWT RS256 filter, CORS, CSRF, stateless session	Backend	1.5
1.7	JwtService: token generation, validation, blacklist	Backend	1.6
1.8	AuthController + AuthService: login, logout, refresh	Backend	1.7
1.9	Password management: bcrypt hashing, account lockout (5 attempts / 15 min), force-change flag	Backend	1.8
1.10	Forgot password / reset password flow: token generation, email trigger, single-use enforcement	Backend	1.9
1.11	AuditLogService + V7 migration: append-only audit log wired into all auth actions	Backend	1.8
1.12	GlobalExceptionHandler: maps all exceptions to consistent HTTP responses	Backend	1.8
1.13	OpenAPI / Swagger setup (springdoc-openapi)	Backend	1.8
1.14	Backend unit tests: AuthService, JwtService, PasswordResetService	Backend	1.10
1.15	Backend Docker image builds and starts cleanly	Infra	1.13
Exit criteria: POST /api/auth/login returns a JWT cookie; POST /api/auth/logout blacklists it; all auth unit tests pass; Flyway migrations run cleanly on fresh DB.

Phase 2 — Frontend Shell + Auth UI
Goal: Angular application running with shell layout, routing, role guards, and all auth screens. No feature pages yet.

Dependencies: Phase 1 complete (login API must be live).

#	Task	Layer	Depends On
2.1	Angular project scaffold: angular.json, package.json, lazy-loaded module structure	Frontend	1.15
2.2	Core module: AuthService, JwtInterceptor, AuthGuard, RoleGuard, ForcePasswordChangeGuard	Frontend	2.1
2.3	NgRx store: auth slice (user, roles, activeRole, timezone, forcePasswordChange)	Frontend	2.2
2.4	Shell component: top header (logo, search, bell, role-switcher, avatar), collapsible sidebar, breadcrumb, contextual action bar	Frontend	2.3
2.5	Role-driven sidebar: renders nav items based on active role; hides unauthorized sections entirely	Frontend	2.4
2.6	Login page: form, error handling, lockout message, show/hide password, Enter-to-submit	Frontend	2.2
2.7	Forgot password page + Reset password page (token validation on load, strength indicator)	Frontend	2.6
2.8	Change password page: forced-change mode (no navigation away), complexity checklist	Frontend	2.6
2.9	TimezoneService: browser detection, X-User-Timezone header on all write requests	Frontend	2.3
2.10	Shared components: skeleton screens, toast, confirmation dialog, empty state	Frontend	2.4
2.11	LocalDatePipe, RelativeTimePipe	Frontend	2.9
2.12	Frontend Docker image builds; Nginx proxies /api/ to backend correctly	Infra	2.1
2.13	E2E test: login → dashboard redirect; forced password change flow	Frontend	2.8
Exit criteria: User can log in, is redirected to a role-appropriate placeholder dashboard, sidebar shows correct items per role, forced password change blocks navigation, logout clears session.

Phase 3 — Admin & Project Setup
Goal: Admin can manage users, projects, and manager assignments. This must come before employee flows because employees need projects to log against and managers to route approvals to.

Dependencies: Phase 1 + Phase 2 complete.

#	Task	Layer	Depends On
3.1	Flyway V2: projects, project_assignments (already in Phase 1 — verify complete)	Database	1.4
3.2	Flyway V5: manager_assignments	Database	1.4
3.3	ProjectService + ProjectController: CRUD, archive/restore, employee assignment	Backend	3.1
3.4	ManagerAssignmentService + ManagerAssignmentController: assign, reassign, circular-check, org chart	Backend	3.2
3.5	UserController (Admin endpoints): create, edit, deactivate, reactivate, reset password, bulk CSV import	Backend	1.5
3.6	SystemConfigController + V10 migration: read/write config (hours thresholds, reminder schedule, weekend toggle, edit window)	Backend	1.3
3.7	Admin — User Management UI: list, search/filter, create/edit slide-in form, deactivate/reactivate, bulk import	Frontend	3.5
3.8	Admin — Project Management UI: list, create/edit/archive/restore, employee assignment	Frontend	3.3
3.9	Admin — Manager Assignments UI: assign/reassign, org chart view	Frontend	3.4
3.10	Admin — System Config UI: editable settings form	Frontend	3.6
3.11	Unit tests: ManagerAssignmentService (circular detection), ProjectService (archive rules)	Backend	3.4
Exit criteria: Admin can create a user, assign a manager, create a project, assign the user to the project. New user receives welcome email and can log in with forced password change.

Phase 4 — Notification Infrastructure
Goal: Email and in-app notification plumbing in place. All subsequent phases depend on this for their notification triggers.

Dependencies: Phase 1 complete (users exist); Phase 2 complete (WebSocket client shell ready).

#	Task	Layer	Depends On
4.1	Flyway V8: notifications table	Database	1.4
4.2	Flyway V9: reminder_logs table	Database	1.4
4.3	JavaMailSender configuration: SMTP host/port/credentials from env vars, retry with exponential backoff	Backend	1.3
4.4	Email template engine: Thymeleaf HTML templates with variable substitution for all notification types	Backend	4.3
4.5	NotificationService: create in-app notification (DB insert) + send email	Backend	4.4
4.6	Spring WebSocket (STOMP) config: /ws endpoint, /user/queue/notifications destination	Backend	4.5
4.7	WebSocketNotificationSender: push to connected user on notification create	Backend	4.6
4.8	NotificationController: GET last 20, mark read, mark all read	Backend	4.5
4.9	Admin — test email feature (verify SMTP config)	Backend	4.3
4.10	NgRx notifications store slice: unread count, notification list	Frontend	2.3
4.11	NotificationService (Angular): REST load + WebSocket STOMP subscription	Frontend	4.10
4.12	Notification Panel component: slide-in panel, unread badge, mark read, deep-links	Frontend	4.11
4.13	Unit tests: NotificationService (email retry), WebSocket delivery	Backend	4.7
Exit criteria: A notification created in the backend appears in the Angular notification panel in real time without page refresh. Test email sends successfully from Admin settings.

Phase 5 — Employee Timesheet Flow
Goal: Employees can log, edit, delete, and view their own timesheet entries. Core business logic including auto-approval, overtime detection, and Day_Status computation.

Dependencies: Phase 3 (projects + manager assignments exist); Phase 4 (notifications ready).

#	Task	Layer	Depends On
5.1	Flyway V3: timesheet_entries (with all indexes)	Database	1.4
5.2	Flyway V4: approval_actions	Database	1.4
5.3	DayStatusComputer: stateless utility, precedence rules (REJECTED > CLARIF > PENDING > APPROVED)	Backend	5.1
5.4	OvertimeValidator: 8-hr warning flag, 9-hr justification enforcement	Backend	5.1
5.5	TimesheetService.submitEntries(): auto-approval (< 1 hr), PENDING assignment, manager capture at submission time, overtime justification storage	Backend	5.3, 5.4, 4.5
5.6	TimesheetService.editEntry(): PENDING/CLARIF only, status reset, auto-approved → PENDING on hours increase	Backend	5.5
5.7	TimesheetService.deleteEntry(): PENDING only	Backend	5.5
5.8	TimesheetController: submit, edit, delete, get week, get day, get history (paginated + filtered), dashboard KPIs	Backend	5.5
5.9	TimezoneUtil: UTC ↔ local date conversion, EOD check	Backend	5.8
5.10	Employee Dashboard component: KPI cards, donut chart, recent activity, missed dates list	Frontend	5.8
5.11	Log Time component: task entry form, running total, 8-hr warning, 9-hr overtime justification, submit/reset	Frontend	5.8
5.12	Weekly View component: 7-column grid, Day_Status badges, holiday markers, missed-date CTAs, expand day	Frontend	5.8
5.13	History component: paginated table, filters (date/project/status), CSV export	Frontend	5.8
5.14	Property-based tests: P5 (future date), P6 (overtime justification), P7 (auto-approval), P8 (edit window), P9 (edit permission + status reset), P10 (Day_Status precedence)	Backend	5.5
Exit criteria: Employee can log tasks, see Day_Status update correctly, edit/delete PENDING entries, see overtime warning at 8 hrs and justification field at 9 hrs, auto-approved tasks show "Auto-Approved" label.

Phase 6 — Holiday Calendar
Goal: HR/Admin can manage holidays; holidays are excluded from missed-date detection and shown in the weekly view.

Dependencies: Phase 3 (HR/Admin roles exist); Phase 5 (weekly view exists to show holidays).

#	Task	Layer	Depends On
6.1	Flyway V11: holiday_calendar table	Database	1.4
6.2	HolidayCalendarService: CRUD, bulk CSV import, retroactive holiday edge case (clear missed flags)	Backend	6.1
6.3	HolidayCalendarController: HR/Admin write, all roles read	Backend	6.2
6.4	Integrate holiday check into TimesheetService: is_holiday flag on entries, exclude from missed-date logic	Backend	6.2, 5.5
6.5	Holiday Calendar UI (HR + Admin): table view, add/edit/delete slide-in form, bulk CSV import	Frontend	6.3
6.6	Weekly View update: holiday day styling (teal background, 🏖️ icon, "Log Time" button, no missed-date CTA)	Frontend	6.4
6.7	Property-based test: P13 (holiday write access control), P14 (holiday exclusion from missed-date)	Backend	6.2
Exit criteria: HR adds a holiday; that date shows 🏖️ in the weekly view for all employees; no missed-date flag or reminder fires for that date.

Phase 7 — Manager Approval Flow
Goal: Managers can review, approve, reject, and request clarification on employee timesheet entries.

Dependencies: Phase 5 (timesheet entries exist); Phase 4 (notifications ready); Phase 6 (holiday calendar for correct weekly view).

#	Task	Layer	Depends On
7.1	ApprovalService: approve (task + day bulk), reject (mandatory reason), self-approval prevention + skip-level routing	Backend	5.5, 4.5
7.2	ApprovalController: all approval endpoints	Backend	7.1
7.3	Flyway V6: clarification_messages	Database	1.4
7.4	ClarificationService + ClarificationController: open thread, post message, read-only after close	Backend	7.3, 4.5
7.5	Manager Dashboard component: KPI cards, team summary table, send reminder buttons	Frontend	7.2
7.6	Team Review component: weekly calendar (Mon–Sun), expand day, task-level actions (approve/reject/clarify), overtime badge + justification inline, bulk approve/reject	Frontend	7.2
7.7	Clarification Thread component: inline slide-in panel, message bubbles, approve/reject from thread	Frontend	7.4
7.8	Property-based tests: P11 (APPROVED immutability), P12 (self-approval prevention)	Backend	7.1
Exit criteria: Manager can approve/reject individual tasks and entire days; self-approval is blocked and re-routed; clarification thread opens, both parties can post, thread locks after resolution; Day_Status updates in real time.

Phase 8 — Scheduler & Reminders
Goal: Automated missed-date detection fires at 5 PM per user timezone; HR and managers can send manual reminders.

Dependencies: Phase 5 (timesheet entries); Phase 6 (holiday calendar); Phase 4 (notification infrastructure).

#	Task	Layer	Depends On
8.1	MissedDateDetectionJob: runs every minute, checks per-user 5 PM trigger, skips weekends and holidays, creates missed-date notifications	Backend	5.9, 6.2, 4.5
8.2	PendingApprovalReminderJob: fires for managers with pending items > 2 business days	Backend	7.1, 4.5
8.3	Distributed scheduler lock (DB-based) to prevent duplicate execution on multi-instance deployments	Backend	8.1
8.4	ReminderController: HR send missing-entry reminder (org-wide), HR send pending-approval reminder; Manager send missing-entry reminder (direct reports), Manager send individual reminder	Backend	4.5
8.5	HR Reminders UI: send buttons with email preview modal, reminder log	Frontend	8.4
8.6	Manager Dashboard — "Send Reminder" buttons wired to ReminderController	Frontend	8.4
8.7	Unit tests: MissedDateDetectionJob (weekend skip, holiday skip, already-logged skip, timezone boundary)	Backend	8.1
Exit criteria: At 5 PM in an employee's local timezone, if no entries exist for that day (and it's not a weekend or holiday), the employee and their manager receive a missed-date notification. HR and managers can send manual reminders with email preview.

Phase 9 — HR Dashboard & Reports
Goal: HR has org-wide visibility and can generate compliance reports.

Dependencies: Phase 5 (timesheet data); Phase 6 (holiday calendar for compliance calculation); Phase 8 (reminders).

#	Task	Layer	Depends On
9.1	HrDashboardService: org-wide KPIs (compliance rate, pending count, avg hours), department breakdown	Backend	5.8, 6.2
9.2	HrController (dashboard + employee daily summary — no task detail)	Backend	9.1
9.3	Flyway V13: export_jobs table	Database	1.4
9.4	ReportService: async report generation (Weekly Compliance, Monthly Hours, Project Distribution, Attendance Overview); ExportJob tracking	Backend	9.1
9.5	ReportController: trigger async report, poll status, download	Backend	9.4
9.6	HR Dashboard component: KPI cards, department breakdown table, drill-down to employee daily summary	Frontend	9.2
9.7	HR Reports component: report type selector, date range, async generation with progress indicator, download	Frontend	9.5
9.8	Manager Reports component: team-scoped CSV/PDF export with filters	Frontend	7.2
Exit criteria: HR dashboard shows correct compliance rate (holidays excluded); HR can generate a Weekly Compliance Report and download it as CSV/PDF; HR cannot see individual task descriptions.

Phase 10 — Admin Audit Log & Final Hardening
Goal: Audit log viewer, final security hardening, performance validation, and production readiness.

Dependencies: All previous phases complete (audit log entries exist from all flows).

#	Task	Layer	Depends On
10.1	AuditLogController: paginated query with filters (date, actor, action type, entity), CSV export	Backend	1.11
10.2	Admin Audit Log UI: filterable table, row expand (before/after JSON), async CSV export	Frontend	10.1
10.3	OWASP Top 10 review: input sanitization audit, CSP headers, HSTS, security header verification	Backend + Frontend	All
10.4	Performance testing (k6): 500 concurrent users, verify 95th percentile < 500ms reads / < 1s writes	Infra	All
10.5	Index verification: confirm all slow queries use indexes; no full table scans in production query plans	Database	All
10.6	Property-based tests: P1 (JWT invalidation), P2 (role inheritance), P3 (reset token single-use), P4 (password complexity), P15 (circular assignment), P16 (UTC round-trip)	Backend	All
10.7	Playwright E2E: all 8 acceptance scenarios from requirements	Frontend	All
10.8	Accessibility audit: axe-core on all primary flows, WCAG 2.1 AA verification	Frontend	All
10.9	Production Docker Compose (docker-compose.prod.yml): pre-built images, no exposed DB port, resource limits	Infra	All
10.10	CI pipeline verification: path-based triggers confirmed working, all jobs green on main	Infra	All
Exit criteria: All 16 correctness properties pass; all 8 acceptance scenarios pass E2E; k6 load test meets performance targets; OWASP scan shows no High/Critical findings; production Docker Compose starts cleanly from GHCR images.

Phase Summary
gantt
    title TMS Construction Phases
    dateFormat  YYYY-MM-DD
    axisFormat  Phase %s

    section Foundation
    Phase 1 — Backend + DB Foundation     :p1, 2025-01-01, 5d
    Phase 2 — Frontend Shell + Auth UI    :p2, after p1, 4d

    section Core Setup
    Phase 3 — Admin & Project Setup       :p3, after p2, 4d
    Phase 4 — Notification Infrastructure :p4, after p2, 3d

    section Business Logic
    Phase 5 — Employee Timesheet Flow     :p5, after p3, 5d
    Phase 6 — Holiday Calendar            :p6, after p5, 2d
    Phase 7 — Manager Approval Flow       :p7, after p5, 4d
    Phase 8 — Scheduler & Reminders       :p8, after p6, 3d

    section Reporting & Hardening
    Phase 9 — HR Dashboard & Reports      :p9, after p8, 3d
    Phase 10 — Audit Log & Hardening      :p10, after p9, 4d
Critical Path
The critical path through the system is:

Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10

Phases 3 and 4 can run in parallel after Phase 2. Phases 6 and 7 can start in parallel after Phase 5. Phase 8 requires both 6 and 7 to be complete before the scheduler can correctly handle holidays and approval state.

End of Design Document