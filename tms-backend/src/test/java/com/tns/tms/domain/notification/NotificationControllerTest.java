package com.tns.tms.domain.notification;

import com.tns.tms.config.TestSecurityConfig;
import com.tns.tms.config.WithMockTmsUser;
import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean(name = "filterUserRepo") UserRepository filterUserRepo;
    @MockBean NotificationRepository notificationRepository;

    private User currentUser;
    private Notification notification;

    @BeforeEach
    void setUp() {
        currentUser = WithMockTmsUser.employee(1L);
        notification = Notification.builder()
                .id(10L).userId(1L).type("ENTRY_APPROVED")
                .message("Your entry was approved").read(false)
                .deepLink("/employee/history").createdAt(Instant.now()).build();
    }

    @Test
    void getNotifications_returns200WithList() throws Exception {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications")
                        .with(WithMockTmsUser.as(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("ENTRY_APPROVED"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void getNotifications_empty_returns200EmptyArray() throws Exception {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/notifications")
                        .with(WithMockTmsUser.as(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void markRead_ownNotification_returns200AndSaves() throws Exception {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        mockMvc.perform(post("/api/notifications/10/read")
                        .with(WithMockTmsUser.as(currentUser)))
                .andExpect(status().isOk());

        verify(notificationRepository).save(any());
    }

    @Test
    void markRead_otherUsersNotification_doesNotSave() throws Exception {
        Notification otherNotif = Notification.builder()
                .id(20L).userId(99L).type("X").message("X").build();
        when(notificationRepository.findById(20L)).thenReturn(Optional.of(otherNotif));

        mockMvc.perform(post("/api/notifications/20/read")
                        .with(WithMockTmsUser.as(currentUser)))
                .andExpect(status().isOk());

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllRead_returns200WithCount() throws Exception {
        when(notificationRepository.markAllReadByUserId(1L)).thenReturn(5);

        mockMvc.perform(post("/api/notifications/read-all")
                        .with(WithMockTmsUser.as(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(5));
    }

    @Test
    void getUnreadCount_returns200WithCount() throws Exception {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(WithMockTmsUser.as(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }
}
