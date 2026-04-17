-- V4: Create TMS4.projects table
CREATE TABLE TMS4.projects (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    name       NVARCHAR(255) NOT NULL,
    code       NVARCHAR(50)  NOT NULL,
    client     NVARCHAR(255),
    start_date DATE,
    end_date   DATE,
    status     NVARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                   CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE','ARCHIVED')),
    created_at DATETIME2     NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT uq_projects_name UNIQUE (name),
    CONSTRAINT uq_projects_code UNIQUE (code)
);

CREATE INDEX idx_projects_status ON TMS4.projects(status);
