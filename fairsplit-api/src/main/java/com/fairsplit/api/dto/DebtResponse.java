package com.fairsplit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DebtResponse(String from, String to, BigDecimal amount) {}