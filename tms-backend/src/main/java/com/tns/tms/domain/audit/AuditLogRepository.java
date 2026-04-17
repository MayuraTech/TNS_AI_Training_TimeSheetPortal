package com.tns.tms.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:actorId IS NULL OR a.actorId = :actorId)
        AND (:actionType IS NULL OR a.actionType = :actionType)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findWithFilters(
        @Param("actorId") Long actorId,
        @Param("actionType") String actionType,
        @Param("entityType") String entityType,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );
}
