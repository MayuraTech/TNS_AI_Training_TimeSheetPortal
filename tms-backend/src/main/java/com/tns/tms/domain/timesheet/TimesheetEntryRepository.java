package com.tns.tms.domain.timesheet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

    List<TimesheetEntry> findByUserIdAndDate(Long userId, LocalDate date);

    List<TimesheetEntry> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    @Query("""
        SELECT te FROM TimesheetEntry te
        WHERE te.user.id = :userId
        AND (:from IS NULL OR te.date >= :from)
        AND (:to IS NULL OR te.date <= :to)
        AND (:status IS NULL OR te.status = :status)
        AND (:projectId IS NULL OR te.project.id = :projectId)
        ORDER BY te.date DESC
        """)
    Page<TimesheetEntry> findWithFilters(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("status") ApprovalStatus status,
        @Param("projectId") Long projectId,
        Pageable pageable
    );

    @Query("""
        SELECT SUM(te.hours) FROM TimesheetEntry te
        WHERE te.user.id = :userId AND te.date = :date
        """)
    java.math.BigDecimal sumHoursByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("""
        SELECT te FROM TimesheetEntry te
        WHERE te.managerIdAtSubmission = :managerId
        AND te.status = 'PENDING'
        AND te.user.id IN :employeeIds
        """)
    List<TimesheetEntry> findPendingForManager(
        @Param("managerId") Long managerId,
        @Param("employeeIds") List<Long> employeeIds
    );

    @Query("""
        SELECT DISTINCT te.date FROM TimesheetEntry te
        WHERE te.user.id = :userId
        AND te.date BETWEEN :from AND :to
        """)
    List<LocalDate> findDistinctDatesByUserAndRange(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
