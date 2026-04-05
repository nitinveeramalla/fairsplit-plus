package com.fairsplit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordSettlementRequest(UUID groupId, UUID paidToId, BigDecimal amount, String note) {}
