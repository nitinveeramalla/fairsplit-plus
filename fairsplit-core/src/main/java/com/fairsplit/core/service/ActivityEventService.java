package com.fairsplit.core.service;

import com.fairsplit.core.entity.Group;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.entity.ActivityEvent;
import com.fairsplit.core.entity.ActivityEventType;
import com.fairsplit.core.repository.ActivityEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityEventService {

    private final ActivityEventRepository activityEventRepository;
    private final ObjectMapper objectMapper;

    public void log(Group group, User actor, ActivityEventType eventType, Map<String, Object> metadata) {
        ActivityEvent event = new ActivityEvent();
        event.setGroup(group);
        event.setActor(actor);
        event.setEventType(eventType);
        try {
            event.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            event.setMetadata("{}");
        }
        activityEventRepository.save(event);
    }

    public List<ActivityEvent> getGroupActivity(UUID groupId) {
        return activityEventRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }
}