-- V12: Create TMS4.reminder_logs table
CREATE TABLE TMS4.reminder_logs (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    sent_by          BIGINT        NOT NULL,
    sender_role      NVARCHAR(50)  NOT NULL,
    recipient_type   NVARCHAR(100) NOT NULL,
    recipient_count  INT           NOT NULL DEFAULT 0,
    sent_at          DATETIME2     NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_rl_sent_by FOREIGN KEY (sent_by) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_rl_sent_by ON TMS4.reminder_logs(sent_by);
CREATE INDEX idx_rl_sent_at ON TMS4.reminder_logs(sent_at);
