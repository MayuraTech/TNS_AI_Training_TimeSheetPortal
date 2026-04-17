-- V15: Create TMS4.export_jobs table
CREATE TABLE TMS4.export_jobs (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    requested_by  BIGINT        NOT NULL,
    report_type   NVARCHAR(100) NOT NULL,
    status        NVARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                      CONSTRAINT chk_ej_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    file_path     NVARCHAR(500),
    created_at    DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    completed_at  DATETIME2,

    CONSTRAINT fk_ej_requested_by FOREIGN KEY (requested_by) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_ej_requested_by ON TMS4.export_jobs(requested_by);
CREATE INDEX idx_ej_status       ON TMS4.export_jobs(status);
