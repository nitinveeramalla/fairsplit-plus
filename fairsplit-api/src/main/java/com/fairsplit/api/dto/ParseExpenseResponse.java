package com.fairsplit.api.dto;

import com.fairsplit.integrations.ai.ExpenseParserService;

import java.util.UUID;

public record ParseExpenseResponse(
        ExpenseParserService.ExpenseParseResult parsed,
        UUID groupId,
        boolean autoCreated,
        UUID expenseId  // non-null if autoCreated=true
) {}
