package com.tns.tms.domain.clarification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClarificationRepository extends JpaRepository<ClarificationMessage, Long> {
    List<ClarificationMessage> findByEntryIdOrderByCreatedAtAsc(Long entryId);
}
