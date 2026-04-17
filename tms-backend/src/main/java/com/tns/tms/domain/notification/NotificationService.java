package com.tns.tms.domain.notification;

import com.tns.tms.domain.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    public NotificationService(JavaMailSender mailSender,
                                NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createInAppNotification(Long userId, String type, String message, String deepLink) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .deepLink(deepLink)
                .build();
        return notificationRepository.save(notification);
    }

    @Retryable(retryFor = MailException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email sent to {}: {}", to, subject);
    }

    @Recover
    public void recoverEmail(MailException ex, String to, String subject, String body) {
        log.error("Failed to send email to {} after 3 attempts. Subject: {}", to, subject, ex);
    }

    public void sendAccountLockedEmail(User user) {
        String subject = "TMS Account Locked";
        String body = String.format(
            "Dear %s,\n\nYour TMS account has been temporarily locked due to multiple failed login attempts.\n" +
            "Please try again in 15 minutes or contact your administrator.\n\nTMS Team",
            user.getFullName()
        );
        sendEmail(user.getEmail(), subject, body);
    }

    public void sendWelcomeEmail(User user, String tempPassword) {
        String subject = "Welcome to TMS — Your Account Details";
        String body = String.format(
            "Dear %s,\n\nYour TMS account has been created.\n" +
            "Email: %s\nTemporary Password: %s\n\n" +
            "Please log in and change your password immediately.\n\nTMS Team",
            user.getFullName(), user.getEmail(), tempPassword
        );
        sendEmail(user.getEmail(), subject, body);
    }

    public void sendPasswordResetEmail(User user, String resetLink) {
        String subject = "TMS Password Reset Request";
        String body = String.format(
            "Dear %s,\n\nClick the link below to reset your password (valid for 1 hour):\n%s\n\n" +
            "If you did not request this, please ignore this email.\n\nTMS Team",
            user.getFullName(), resetLink
        );
        sendEmail(user.getEmail(), subject, body);
    }
}
