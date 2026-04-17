-- V9: Create TMS4.clarification_messages table
CREATE TABLE TMS4.clarification_messages (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    entry_id   BIGINT         NOT NULL,
    author_id  BIGINT         NOT NULL,
    message    NVARCHAR(1000) NOT NULL,
    created_at DATETIME2      NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_cm_entry  FOREIGN KEY (entry_id)  REFERENCES TMS4.timesheet_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_author FOREIGN KEY (author_id) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_cm_entry_id ON TMS4.clarification_messages(entry_id);
