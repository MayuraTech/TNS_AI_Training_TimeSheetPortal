package com.tns.tms.domain.timesheet;

import com.tns.tms.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validates overtime rules:
 * - Warning at > 8 hours (soft)
 * - Justification required at > 9 hours (hard block)
 */
@Component
public class OvertimeValidator {

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("8.0");
    private static final BigDecimal OVERTIME_THRESHOLD = new BigDecimal("9.0");

    /**
     * Returns true if daily total exceeds the warning threshold (> 8 hrs).
     */
    public boolean isOverWarningThreshold(BigDecimal dailyTotal) {
        return dailyTotal.compareTo(WARNING_THRESHOLD) > 0;
    }

    /**
     * Returns true if daily total exceeds the overtime threshold (> 9 hrs).
     */
    public boolean isOverOvertimeThreshold(BigDecimal dailyTotal) {
        return dailyTotal.compareTo(OVERTIME_THRESHOLD) > 0;
    }

    /**
     * Validates that overtime justification is provided when daily total > 9 hrs.
     * Throws ValidationException if justification is missing or too short.
     */
    public void validateOvertimeJustification(BigDecimal dailyTotal, String justification) {
        if (isOverOvertimeThreshold(dailyTotal)) {
            if (justification == null || justification.trim().length() < 10) {
                throw new ValidationException(
                    "Please provide a reason for logging more than 9 hours. " +
                    "Justification must be at least 10 characters."
                );
            }
            if (justification.trim().length() > 300) {
                throw new ValidationException(
                    "Overtime justification must not exceed 300 characters."
                );
            }
        }
    }
}
