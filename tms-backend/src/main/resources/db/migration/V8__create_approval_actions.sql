-- V8: Create TMS4.approval_actions table
CREATE TABLE TMS4.approval_actions (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    entry_id   BIGINT        NOT NULL,
    actor_id   BIGINT        NOT NULL,
    action     NVARCHAR(30)  NOT NULL
                   CONSTRAINT chk_aa_action CHECK (action IN ('APPROVED','REJECTED','CLARIFICATION_REQUESTED')),
    reason     NVARCHAR(500),
    created_at DATETIME2     NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_aa_entry FOREIGN KEY (entry_id) REFERENCES TMS4.timesheet_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_aa_actor FOREIGN KEY (actor_id) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_aa_entry_id ON TMS4.approval_actions(entry_id);
CREATE INDEX idx_aa_actor_id ON TMS4.approval_actions(actor_id);
