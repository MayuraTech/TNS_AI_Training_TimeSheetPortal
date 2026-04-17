export type ApprovalStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CLARIFICATION_REQUESTED'
  | 'AUTO_APPROVED';

export type DayStatus =
  | 'APPROVED'
  | 'REJECTED'
  | 'CLARIFICATION_REQUESTED'
  | 'PENDING'
  | 'NO_ENTRIES';

export interface TimesheetEntry {
  id: number;
  userId: number;
  projectId: number;
  projectName: string;
  managerIdAtSubmission: number;
  date: string; // ISO date
  taskName: string;
  taskDescription?: string;
  hours: number;
  status: ApprovalStatus;
  overtimeJustification?: string;
  autoApproved: boolean;
  submittedAt: string;
  updatedAt: string;
}

export interface TimesheetEntryRequest {
  projectId: number;
  date: string;
  taskName: string;
  taskDescription?: string;
  hours: number;
  overtimeJustification?: string;
}

export interface DaySummary {
  date: string;
  dayStatus: DayStatus;
  totalHours: number;
  entries: TimesheetEntry[];
  isHoliday: boolean;
  holidayName?: string;
  isMissed: boolean;
  isWeekend: boolean;
}

export interface WeeklySummary {
  weekStart: string;
  weekEnd: string;
  days: DaySummary[];
  totalHours: number;
}

export interface EmployeeDashboard {
  totalHoursThisWeek: number;
  pendingCount: number;
  approvedCount: number;
  missedDates: string[];
  recentActivity: ActivityItem[];
}

export interface ActivityItem {
  id: number;
  type: string;
  message: string;
  timestamp: string;
  deepLink?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
