-- V6: Create TMS4.manager_assignments table
CREATE TABLE TMS4.manager_assignments (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    employee_id    BIGINT    NOT NULL,
    manager_id     BIGINT    NOT NULL,
    effective_from DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    effective_to   DATETIME2,

    CONSTRAINT fk_ma_employee FOREIGN KEY (employee_id) REFERENCES TMS4.users(id),
    CONSTRAINT fk_ma_manager  FOREIGN KEY (manager_id)  REFERENCES TMS4.users(id),
    CONSTRAINT chk_ma_no_self CHECK (employee_id <> manager_id)
);

CREATE INDEX idx_ma_employee_id ON TMS4.manager_assignments(employee_id);
CREATE INDEX idx_ma_manager_id  ON TMS4.manager_assignments(manager_id);
