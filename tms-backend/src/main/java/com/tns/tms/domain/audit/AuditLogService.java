package com.tns.tms.domain.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an audit event. Uses REQUIRES_NEW to ensure audit is persisted
     * even if the calling transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long actorId, String actionType, String entityType, Long entityId,
                    String beforeValue, String afterValue) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorId(actorId)
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .beforeValue(beforeValue)
                    .afterValue(afterValue)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: actor={}, action={}, entity={}/{}",
                    actorId, actionType, entityType, entityId, e);
        }
    }
}
