-- V10: Create TMS4.audit_logs table (immutable — no UPDATE/DELETE)
CREATE TABLE TMS4.audit_logs (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    actor_id     BIGINT         NOT NULL,
    action_type  NVARCHAR(100)  NOT NULL,
    entity_type  NVARCHAR(100)  NOT NULL,
    entity_id    BIGINT,
    before_value NVARCHAR(MAX),
    after_value  NVARCHAR(MAX),
    created_at   DATETIME2      NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT fk_al_actor FOREIGN KEY (actor_id) REFERENCES TMS4.users(id)
);

CREATE INDEX idx_al_actor_id    ON TMS4.audit_logs(actor_id);
CREATE INDEX idx_al_action_type ON TMS4.audit_logs(action_type);
CREATE INDEX idx_al_entity_type ON TMS4.audit_logs(entity_type);
CREATE INDEX idx_al_created_at  ON TMS4.audit_logs(created_at);
