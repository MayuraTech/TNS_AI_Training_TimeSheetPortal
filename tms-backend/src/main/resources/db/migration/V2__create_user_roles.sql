-- V2: Create TMS4.user_roles join table
CREATE TABLE TMS4.user_roles (
    user_id BIGINT       NOT NULL,
    role    NVARCHAR(50) NOT NULL
                CONSTRAINT chk_user_roles_role CHECK (role IN ('EMPLOYEE','MANAGER','HR','ADMIN')),

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES TMS4.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON TMS4.user_roles(user_id);
