package com.fairsplit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DebtResponse(UUID from, UUID to, BigDecimal amount) {
}
