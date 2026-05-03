package com.fairsplit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public record CreateExpenseRequest(
        UUID groupId,
        BigDecimal amount,
        String description,
        String currency,
        String splitType,
        List<SplitEntry> splits,
        UUID paidById
) {
    public record SplitEntry(UUID userId, BigDecimal value) {}
}