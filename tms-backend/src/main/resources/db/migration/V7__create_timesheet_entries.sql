-- V7: Create TMS4.timesheet_entries table
CREATE TABLE TMS4.timesheet_entries (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                  BIGINT         NOT NULL,
    project_id               BIGINT         NOT NULL,
    manager_id_at_submission BIGINT,
    date                     DATE           NOT NULL,
    task_name                NVARCHAR(100)  NOT NULL,
    task_description         NVARCHAR(500),
    hours                    DECIMAL(4,1)   NOT NULL
                                 CONSTRAINT chk_te_hours CHECK (hours >= 0.5 AND hours <= 9.0),
    status                   NVARCHAR(30)   NOT NULL DEFAULT 'PENDING'
                                 CONSTRAINT chk_te_status CHECK (status IN (
                                     'PENDING','APPROVED','REJECTED','CLARIFICATION_REQUESTED','AUTO_APPROVED'
                                 )),
    overtime_justification   NVARCHAR(300),
    is_auto_approved         BIT            NOT NULL DEFAULT 0,
    submitted_at             DATETIME2      NOT NULL DEFAULT GETUTCDATE(),
    updated_at               DATETIME2      NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_te_user    FOREIGN KEY (user_id)    REFERENCES TMS4.users(id),
    CONSTRAINT fk_te_project FOREIGN KEY (project_id) REFERENCES TMS4.projects(id),
    CONSTRAINT fk_te_manager FOREIGN KEY (manager_id_at_submission) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_te_user_id    ON TMS4.timesheet_entries(user_id);
CREATE INDEX idx_te_date       ON TMS4.timesheet_entries(date);
CREATE INDEX idx_te_status     ON TMS4.timesheet_entries(status);
CREATE INDEX idx_te_manager_id ON TMS4.timesheet_entries(manager_id_at_submission);
CREATE INDEX idx_te_user_date  ON TMS4.timesheet_entries(user_id, date);
