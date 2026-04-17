package com.tns.tms.domain.holiday;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/holidays")
@Tag(name = "Holiday Calendar", description = "Organisation holiday calendar management")
public class HolidayCalendarController {

    private final HolidayCalendarService holidayCalendarService;

    public HolidayCalendarController(HolidayCalendarService holidayCalendarService) {
        this.holidayCalendarService = holidayCalendarService;
    }

    @GetMapping
    @Operation(summary = "List holiday calendar entries")
    public ResponseEntity<List<HolidayCalendar>> getHolidays(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfYear(1);
        if (to == null) to = LocalDate.now().withDayOfYear(1).plusYears(1).minusDays(1);
        return ResponseEntity.ok(holidayCalendarService.getHolidays(from, to));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @Operation(summary = "Create holiday entry")
    public ResponseEntity<HolidayCalendar> addHoliday(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        HolidayCalendar holiday = holidayCalendarService.addHoliday(
                currentUser,
                body.get("name"),
                LocalDate.parse(body.get("date")),
                HolidayType.valueOf(body.get("type")),
                body.get("applicableTo")
        );
        return ResponseEntity.ok(holiday);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @Operation(summary = "Delete holiday entry")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        holidayCalendarService.deleteHoliday(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
