-- V13: Create TMS4.holiday_calendar table
CREATE TABLE TMS4.holiday_calendar (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(255) NOT NULL,
    date            DATE          NOT NULL,
    type            NVARCHAR(50)  NOT NULL
                        CONSTRAINT chk_hc_type CHECK (type IN ('PUBLIC','COMPANY','OPTIONAL')),
    applicable_to   NVARCHAR(255) NOT NULL DEFAULT 'ALL',
    created_by      BIGINT        NOT NULL,
    created_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_hc_created_by FOREIGN KEY (created_by) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_hc_date ON TMS4.holiday_calendar(date);
