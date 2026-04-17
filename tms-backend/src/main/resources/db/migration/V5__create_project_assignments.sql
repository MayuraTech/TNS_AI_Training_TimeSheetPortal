-- V5: Create TMS4.project_assignments table
CREATE TABLE TMS4.project_assignments (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    project_id  BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    assigned_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT uq_project_assignments UNIQUE (project_id, user_id),
    CONSTRAINT fk_pa_project FOREIGN KEY (project_id) REFERENCES TMS4.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_user    FOREIGN KEY (user_id)    REFERENCES TMS4.users(id)    ON DELETE CASCADE
);

CREATE INDEX idx_pa_project_id ON TMS4.project_assignments(project_id);
CREATE INDEX idx_pa_user_id    ON TMS4.project_assignments(user_id);
