package com.tns.tms.domain.clarification;

import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.domain.timesheet.ApprovalStatus;
import com.tns.tms.domain.timesheet.TimesheetEntry;
import com.tns.tms.domain.timesheet.TimesheetEntryRepository;
import com.tns.tms.domain.user.User;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClarificationService {

    private final ClarificationRepository clarificationRepository;
    private final TimesheetEntryRepository entryRepository;
    private final NotificationService notificationService;

    public ClarificationService(ClarificationRepository clarificationRepository,
                                  TimesheetEntryRepository entryRepository,
                                  NotificationService notificationService) {
        this.clarificationRepository = clarificationRepository;
        this.entryRepository = entryRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ClarificationMessage> getThread(Long entryId) {
        // Verify entry exists
        entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));
        return clarificationRepository.findByEntryIdOrderByCreatedAtAsc(entryId);
    }

    @Transactional
    public ClarificationMessage postMessage(User author, Long entryId, String message) {
        TimesheetEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + entryId));

        // Thread is read-only once APPROVED or REJECTED
        if (entry.getStatus() == ApprovalStatus.APPROVED
                || entry.getStatus() == ApprovalStatus.AUTO_APPROVED
                || entry.getStatus() == ApprovalStatus.REJECTED) {
            throw new ValidationException("Clarification thread is closed for this entry.");
        }

        ClarificationMessage msg = ClarificationMessage.builder()
                .entry(entry)
                .author(author)
                .message(message)
                .build();
        ClarificationMessage saved = clarificationRepository.save(msg);

        // Notify the other party
        boolean authorIsEmployee = entry.getUser().getId().equals(author.getId());
        if (authorIsEmployee && entry.getManagerIdAtSubmission() != null) {
            // Employee replied — notify manager
            notificationService.createInAppNotification(
                    entry.getManagerIdAtSubmission(),
                    "CLARIFICATION_REPLY",
                    author.getFullName() + " replied to clarification on entry " + entryId,
                    "/manager/team-review/" + entry.getUser().getId()
            );
        } else if (!authorIsEmployee) {
            // Manager posted — notify employee
            notificationService.createInAppNotification(
                    entry.getUser().getId(),
                    "CLARIFICATION_REQUESTED",
                    "Manager requested clarification on your entry for " + entry.getDate(),
                    "/employee/history/" + entryId
            );
        }

        return saved;
    }
}
