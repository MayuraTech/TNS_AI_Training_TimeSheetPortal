package com.tns.tms.domain.timesheet;

import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class OvertimeValidatorTest {

    private OvertimeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OvertimeValidator();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 4.0, 7.5, 8.0})
    void isOverWarningThreshold_atOrBelowWarning_returnsFalse(double hours) {
        assertThat(validator.isOverWarningThreshold(BigDecimal.valueOf(hours))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(doubles = {8.5, 9.0, 9.5})
    void isOverWarningThreshold_aboveWarning_returnsTrue(double hours) {
        assertThat(validator.isOverWarningThreshold(BigDecimal.valueOf(hours))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 8.0, 9.0})
    void isOverOvertimeThreshold_atOrBelowOvertime_returnsFalse(double hours) {
        assertThat(validator.isOverOvertimeThreshold(BigDecimal.valueOf(hours))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.5})
    void isOverOvertimeThreshold_aboveOvertime_returnsTrue(double hours) {
        assertThat(validator.isOverOvertimeThreshold(BigDecimal.valueOf(hours))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "9.5, 'This is a valid justification comment'",
        "10.0, 'Working on critical production issue that required extended hours'"
    })
    void validateOvertimeJustification_validJustification_doesNotThrow(double hours, String justification) {
        assertThatCode(() ->
                validator.validateOvertimeJustification(BigDecimal.valueOf(hours), justification))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.5, 10.0})
    void validateOvertimeJustification_missingJustification_throws(double hours) {
        assertThatThrownBy(() ->
                validator.validateOvertimeJustification(BigDecimal.valueOf(hours), null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("9 hours");
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.5, 10.0})
    void validateOvertimeJustification_tooShortJustification_throws(double hours) {
        assertThatThrownBy(() ->
                validator.validateOvertimeJustification(BigDecimal.valueOf(hours), "short"))
                .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 8.0, 9.0})
    void validateOvertimeJustification_belowThreshold_doesNotRequireJustification(double hours) {
        // Should not throw even without justification
        assertThatCode(() ->
                validator.validateOvertimeJustification(BigDecimal.valueOf(hours), null))
                .doesNotThrowAnyException();
    }
}
