package com.fairsplit.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityEventResponse(
        UUID id,
        String eventType,
        String actorName,
        String metadata,
        LocalDateTime createdAt
) {}