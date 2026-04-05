package com.fairsplit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateExpenseRequest(UUID groupId, BigDecimal amount, String description, String currency) {
}
