-- V14: Create TMS4.system_config table with default seed values
CREATE TABLE TMS4.system_config (
    [key]       NVARCHAR(100) NOT NULL PRIMARY KEY,
    value       NVARCHAR(500) NOT NULL,
    updated_by  BIGINT,
    updated_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_sc_updated_by FOREIGN KEY (updated_by) REFERENCES TMS4.users(id)
);

-- Seed default configuration values
INSERT INTO TMS4.system_config ([key], value) VALUES
    ('work_week_days',              'MON,TUE,WED,THU,FRI'),
    ('weekend_logging_enabled',     'true'),
    ('daily_hours_warning_threshold', '8.0'),
    ('daily_hours_overtime_threshold', '9.0'),
    ('reminder_schedule_time',      '17:00'),
    ('reminder_schedule_days',      'MON,TUE,WED,THU,FRI'),
    ('past_entry_edit_window_days', '30'),
    ('entry_lock_days_after_approval', '0'),
    ('smtp_host',                   'smtp.office365.com'),
    ('smtp_port',                   '587'),
    ('smtp_from_address',           'noreply@thinksolutions.com'),
    ('smtp_from_name',              'TMS Notifications');
