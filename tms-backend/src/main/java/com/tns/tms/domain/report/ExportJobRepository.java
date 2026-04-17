package com.tns.tms.domain.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
    List<ExportJob> findByRequestedByOrderByCreatedAtDesc(Long requestedBy);
}
