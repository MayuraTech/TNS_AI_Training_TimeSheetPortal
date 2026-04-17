-- V1: Create TMS4.users table
CREATE TABLE TMS4.users (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name             NVARCHAR(255)  NOT NULL,
    email                 NVARCHAR(255)  NOT NULL,
    password_hash         NVARCHAR(255)  NOT NULL,
    department            NVARCHAR(255),
    employee_id           NVARCHAR(100),
    timezone              NVARCHAR(100)  NOT NULL DEFAULT 'UTC',
    status                NVARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                              CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','INACTIVE')),
    failed_login_attempts INT            NOT NULL DEFAULT 0,
    locked_until          DATETIME2,
    force_password_change BIT            NOT NULL DEFAULT 1,
    created_at            DATETIME2      NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT uq_users_email       UNIQUE (email),
    CONSTRAINT uq_users_employee_id UNIQUE (employee_id)
);

EXEC sp_addextendedproperty
    @name = N'MS_Description', @value = N'TMS application users',
    @level0type = N'SCHEMA', @level0name = N'TMS4',
    @level1type = N'TABLE',  @level1name = N'users';
