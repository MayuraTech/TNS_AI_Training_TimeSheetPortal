-- V3: Create TMS4.password_reset_tokens table
CREATE TABLE TMS4.password_reset_tokens (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    token_hash NVARCHAR(255) NOT NULL,
    expires_at DATETIME2     NOT NULL,
    used       BIT           NOT NULL DEFAULT 0,
    created_at DATETIME2     NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES TMS4.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_user_id    ON TMS4.password_reset_tokens(user_id);
CREATE INDEX idx_prt_token_hash ON TMS4.password_reset_tokens(token_hash);
