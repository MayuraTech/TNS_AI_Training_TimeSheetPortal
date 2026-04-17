package com.tns.tms.domain.notification;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    @Operation(summary = "Get last 20 notifications for current user")
    public ResponseEntity<List<Notification>> getNotifications(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(0, 20)));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(currentUser.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllRead(@AuthenticationPrincipal User currentUser) {
        int count = notificationRepository.markAllReadByUserId(currentUser.getId());
        return ResponseEntity.ok(Map.of("marked", count));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        long count = notificationRepository.countByUserIdAndReadFalse(currentUser.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
