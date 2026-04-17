package com.tns.tms.shared.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimesheetEntryRequest(
    @NotNull Long projectId,
    @NotNull LocalDate date,
    @NotBlank @Size(max = 100) String taskName,
    @Size(max = 500) String taskDescription,
    @NotNull @DecimalMin("0.5") @DecimalMax("9.0") BigDecimal hours,
    @Size(min = 10, max = 300) String overtimeJustification
) {}
