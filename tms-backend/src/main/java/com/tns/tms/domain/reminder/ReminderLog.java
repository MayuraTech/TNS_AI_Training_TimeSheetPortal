package com.tns.tms.domain.reminder;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reminder_logs", schema = "TMS4")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sent_by", nullable = false)
    private Long sentBy;

    @Column(name = "sender_role", nullable = false)
    private String senderRole;

    @Column(name = "recipient_type", nullable = false)
    private String recipientType;

    @Column(name = "recipient_count", nullable = false)
    @Builder.Default
    private int recipientCount = 0;

    @Column(name = "sent_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();
}
