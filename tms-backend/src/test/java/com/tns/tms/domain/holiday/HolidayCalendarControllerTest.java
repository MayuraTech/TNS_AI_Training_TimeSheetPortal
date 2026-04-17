package com.tns.tms.domain.holiday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HolidayCalendarController.class)
@Import(TestSecurityConfig.class)
class HolidayCalendarControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean HolidayCalendarService holidayCalendarService;

    private User hrUser;
    private User employee;
    private HolidayCalendar christmas;

    @BeforeEach
    void setUp() {
        hrUser   = WithMockTmsUser.hr(1L);
        employee = WithMockTmsUser.employee(2L);
        christmas = HolidayCalendar.builder()
                .id(1L).name("Christmas").date(LocalDate.of(2026, 12, 25))
                .type(HolidayType.PUBLIC).applicableTo("ALL").createdBy(1L).build();
    }

    @Test
    void getHolidays_allRolesCanRead_returns200() throws Exception {
        when(holidayCalendarService.getHolidays(any(), any())).thenReturn(List.of(christmas));

        mockMvc.perform(get("/api/hr/holidays")
                        .with(WithMockTmsUser.as(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Christmas"))
                .andExpect(jsonPath("$[0].type").value("PUBLIC"));
    }

    @Test
    void getHolidays_withDateRange_returns200() throws Exception {
        when(holidayCalendarService.getHolidays(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/hr/holidays")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .with(WithMockTmsUser.as(hrUser)))
                .andExpect(status().isOk());
    }

    @Test
    void addHoliday_hrRole_returns200() throws Exception {
        when(holidayCalendarService.addHoliday(any(), anyString(), any(), any(), anyString()))
                .thenReturn(christmas);

        mockMvc.perform(post("/api/hr/holidays")
                        .with(WithMockTmsUser.as(hrUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Christmas",
                                "date", "2026-12-25",
                                "type", "PUBLIC",
                                "applicableTo", "ALL"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Christmas"));
    }

    @Test
    void addHoliday_duplicateDate_returns400() throws Exception {
        when(holidayCalendarService.addHoliday(any(), anyString(), any(), any(), anyString()))
                .thenThrow(new ValidationException("A holiday already exists for date: 2026-12-25"));

        mockMvc.perform(post("/api/hr/holidays")
                        .with(WithMockTmsUser.as(hrUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Duplicate",
                                "date", "2026-12-25",
                                "type", "PUBLIC",
                                "applicableTo", "ALL"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void deleteHoliday_hrRole_returns204() throws Exception {
        doNothing().when(holidayCalendarService).deleteHoliday(any(), eq(1L));

        mockMvc.perform(delete("/api/hr/holidays/1")
                        .with(WithMockTmsUser.as(hrUser)))
                .andExpect(status().isNoContent());
    }
}
