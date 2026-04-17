package com.tns.tms.domain.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebSocketNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationSender.class);
    private static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Sends a real-time notification to a specific user via WebSocket.
     * Routes to /user/{userId}/queue/notifications
     */
    public void sendToUser(Long userId, Notification notification) {
        try {
            Map<String, Object> payload = Map.of(
                    "id", notification.getId() != null ? notification.getId() : 0L,
                    "type", notification.getType(),
                    "message", notification.getMessage(),
                    "deepLink", notification.getDeepLink() != null ? notification.getDeepLink() : "",
                    "createdAt", notification.getCreatedAt().toString()
            );
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    USER_NOTIFICATION_DESTINATION,
                    payload
            );
            log.debug("WebSocket notification sent to user {}: {}", userId, notification.getType());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage(), e);
        }
    }
}
