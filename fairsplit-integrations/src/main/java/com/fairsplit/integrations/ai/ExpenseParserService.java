package com.fairsplit.integrations.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ExpenseParserService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseParserService.class);

    private final AnthropicChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ExpenseParserService(AnthropicChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    private static final String SYSTEM_PROMPT = """
            You are an expense parsing assistant. Extract expense details from natural language.
            ALWAYS respond with valid JSON only — no preamble, no markdown.

            Output schema:
            {
              "amount": number,
              "currency": "USD",
              "description": "short 3-5 word label",
              "category": "FOOD|TRANSPORT|ACCOMMODATION|UTILITIES|ENTERTAINMENT|GROCERIES|HEALTH|SHOPPING|OTHER",
              "splitType": "EQUAL|EXACT|PERCENTAGE|SHARES",
              "participants": ["name1", "name2"],
              "splits": [{ "participant": "name", "amount": number }],
              "confidence": "HIGH|MEDIUM|LOW",
              "clarificationNeeded": "string or null"
            }

            Rules:
            - If splitType is EQUAL, splits array can be empty
            - If ambiguous, set confidence LOW and clarificationNeeded to a question
            - Always include the payer in participants
            - Currency defaults to USD unless specified
            """;

    public ExpenseParseResult parse(String input, String payerName) {
        log.debug("Parsing expense: {}", input);
        var prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage("Payer: " + payerName + "\nInput: " + input)
        ));
        try {
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            return objectMapper.readValue(response, ExpenseParseResult.class);
        } catch (Exception e) {
            log.error("Failed to parse: {}", input, e);
            return ExpenseParseResult.failed("Could not parse input. Please fill in manually.");
        }
    }

    public record ExpenseParseResult(
            BigDecimal amount, String currency, String description,
            String category, String splitType, List<String> participants,
            List<SplitEntry> splits, String confidence,
            String clarificationNeeded, String errorMessage
    ) {
        public boolean isSuccessful() { return errorMessage == null; }
        public boolean needsClarification() { return clarificationNeeded != null; }

        static ExpenseParseResult failed(String errorMessage) {
            return new ExpenseParseResult(null, null, null, null, null,
                    List.of(), List.of(), "LOW", null, errorMessage);
        }
    }

    public record SplitEntry(String participant, BigDecimal amount) {}
}