-- V16: Additional composite indexes for performance
-- Timesheet entries: user + date range queries (most common query pattern)
CREATE INDEX idx_te_user_date_status ON TMS4.timesheet_entries(user_id, date, status);

-- Timesheet entries: manager pending approvals
CREATE INDEX idx_te_manager_status ON TMS4.timesheet_entries(manager_id_at_submission, status)
    WHERE status = 'PENDING';

-- Audit logs: date range queries
CREATE INDEX idx_al_entity_created ON TMS4.audit_logs(entity_type, entity_id, created_at);

-- Notifications: unread count per user
CREATE INDEX idx_notif_unread ON TMS4.notifications(user_id, is_read, created_at);

-- Manager assignments: active assignments
CREATE INDEX idx_ma_active ON TMS4.manager_assignments(employee_id, effective_to)
    WHERE effective_to IS NULL;
