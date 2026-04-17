package com.tns.tms.domain.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {}
