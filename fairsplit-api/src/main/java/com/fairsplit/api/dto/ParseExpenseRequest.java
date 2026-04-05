package com.fairsplit.api.dto;

import java.util.UUID;

public record ParseExpenseRequest(String input, UUID groupId) {}
