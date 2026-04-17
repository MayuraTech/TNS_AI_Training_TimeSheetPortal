package com.tns.tms.domain.clarification;

import com.tns.tms.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clarifications")
@Tag(name = "Clarifications", description = "Clarification thread management")
public class ClarificationController {

    private final ClarificationService clarificationService;

    public ClarificationController(ClarificationService clarificationService) {
        this.clarificationService = clarificationService;
    }

    @GetMapping("/entries/{entryId}")
    @Operation(summary = "Get clarification thread for an entry")
    public ResponseEntity<List<ClarificationMessage>> getThread(@PathVariable Long entryId) {
        return ResponseEntity.ok(clarificationService.getThread(entryId));
    }

    @PostMapping("/entries/{entryId}")
    @Operation(summary = "Post a message to the clarification thread")
    public ResponseEntity<ClarificationMessage> postMessage(
            @PathVariable Long entryId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        ClarificationMessage msg = clarificationService.postMessage(
                currentUser, entryId, body.get("message"));
        return ResponseEntity.ok(msg);
    }
}
