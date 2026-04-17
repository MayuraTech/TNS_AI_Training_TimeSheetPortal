-- V11: Create TMS4.notifications table
CREATE TABLE TMS4.notifications (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    type       NVARCHAR(100)  NOT NULL,
    message    NVARCHAR(500)  NOT NULL,
    is_read    BIT            NOT NULL DEFAULT 0,
    deep_link  NVARCHAR(500),
    created_at DATETIME2      NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES TMS4.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user_id ON TMS4.notifications(user_id);
CREATE INDEX idx_notif_is_read ON TMS4.notifications(user_id, is_read);
