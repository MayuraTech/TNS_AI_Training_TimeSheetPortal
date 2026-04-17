package com.tns.tms.domain.holiday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {

    @Query("SELECT h.date FROM HolidayCalendar h WHERE h.date BETWEEN :from AND :to")
    Set<LocalDate> findHolidayDatesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    boolean existsByDate(LocalDate date);

    List<HolidayCalendar> findByDateBetweenOrderByDate(LocalDate from, LocalDate to);
}
