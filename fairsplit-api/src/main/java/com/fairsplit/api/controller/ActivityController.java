package com.fairsplit.api.controller;

import com.fairsplit.api.dto.ActivityEventResponse;
import com.fairsplit.core.entity.ActivityEvent;
import com.fairsplit.core.service.ActivityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityEventService activityEventService;

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ActivityEventResponse>> getGroupActivity(@PathVariable UUID groupId) {
        List<ActivityEvent> events = activityEventService.getGroupActivity(groupId);
        List<ActivityEventResponse> response = events.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private ActivityEventResponse toResponse(ActivityEvent event) {
        return new ActivityEventResponse(
                event.getId(),
                event.getEventType().name(),
                event.getActor().getDisplayName(),
                event.getMetadata(),
                event.getCreatedAt()
        );
    }
}
