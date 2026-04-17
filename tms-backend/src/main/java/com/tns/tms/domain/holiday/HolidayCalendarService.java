package com.tns.tms.domain.holiday;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.user.User;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayCalendarService {

    private static final Logger log = LoggerFactory.getLogger(HolidayCalendarService.class);

    private final HolidayCalendarRepository holidayCalendarRepository;
    private final AuditLogService auditLogService;

    public HolidayCalendarService(HolidayCalendarRepository holidayCalendarRepository,
                                    AuditLogService auditLogService) {
        this.holidayCalendarRepository = holidayCalendarRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<HolidayCalendar> getHolidays(LocalDate from, LocalDate to) {
        return holidayCalendarRepository.findByDateBetweenOrderByDate(from, to);
    }

    @Transactional
    public HolidayCalendar addHoliday(User actor, String name, LocalDate date,
                                       HolidayType type, String applicableTo) {
        if (holidayCalendarRepository.existsByDate(date)) {
            throw new ValidationException("A holiday already exists for date: " + date);
        }

        HolidayCalendar holiday = HolidayCalendar.builder()
                .name(name)
                .date(date)
                .type(type)
                .applicableTo(applicableTo != null ? applicableTo : "ALL")
                .createdBy(actor.getId())
                .build();

        HolidayCalendar saved = holidayCalendarRepository.save(holiday);
        auditLogService.log(actor.getId(), "HOLIDAY_ADDED", "HOLIDAY_CALENDAR",
                saved.getId(), null, "date=" + date + ",name=" + name);

        log.info("Holiday added: {} on {} by user {}", name, date, actor.getId());
        return saved;
    }

    @Transactional
    public void deleteHoliday(User actor, Long holidayId) {
        HolidayCalendar holiday = holidayCalendarRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + holidayId));

        auditLogService.log(actor.getId(), "HOLIDAY_DELETED", "HOLIDAY_CALENDAR",
                holidayId, "date=" + holiday.getDate() + ",name=" + holiday.getName(), null);

        holidayCalendarRepository.delete(holiday);
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayCalendarRepository.existsByDate(date);
    }
}
