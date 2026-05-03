package com.fairsplit.api.service;

import com.fairsplit.api.dto.CreateExpenseRequest;
import com.fairsplit.api.dto.ParseExpenseResponse;
import com.fairsplit.core.entity.Expense;
import com.fairsplit.core.entity.ExpenseSplit;
import com.fairsplit.core.entity.Group;
import com.fairsplit.core.entity.GroupMember;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.entity.ActivityEventType;
import com.fairsplit.core.repository.ExpenseRepository;
import com.fairsplit.core.repository.GroupRepository;
import com.fairsplit.core.repository.UserRepository;
import com.fairsplit.core.service.ActivityEventService;
import com.fairsplit.integrations.ai.ExpenseParserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {

    private final GroupRepository groupRepository;

    private final ExpenseRepository expenseRepository;

    private final UserRepository userRepository;

    private final ExpenseParserService expenseParserService;

    private final ActivityEventService activityEventService;

    public ExpenseService(GroupRepository groupRepository, ExpenseRepository expenseRepository,
                          UserRepository userRepository, ExpenseParserService expenseParserService,
                          ActivityEventService activityEventService) {
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.expenseParserService = expenseParserService;
        this.activityEventService = activityEventService;
    }

    public Expense createExpense(CreateExpenseRequest request, UUID paidById) {
        Group group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User paidBy = userRepository.findById(paidById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String splitType = request.splitType() == null ? "EQUAL" : request.splitType();

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidBy)
                .createdBy(paidBy)
                .amount(request.amount())
                .description(request.description())
                .currency(request.currency() != null ? request.currency() : "USD")
                .splitType(Expense.SplitType.valueOf(splitType))
                .build();


        if (splitType.equals("EQUAL")) {
            BigDecimal splitAmount = request.amount().divide(
                    BigDecimal.valueOf(group.getMembers().size()), 2, RoundingMode.HALF_UP);
            for (GroupMember member : group.getMembers()) {
                expense.getSplits().add(ExpenseSplit.builder()
                        .expense(expense)
                        .user(member.getUser())
                        .owedAmount(splitAmount)
                        .build());
            }
        } else if (splitType.equals("EXACT")) {
            BigDecimal sum = request.splits().stream()
                    .map(CreateExpenseRequest.SplitEntry::value)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(request.amount()) != 0) {
                throw new RuntimeException("Exact split amounts must sum to total amount");
            }
            for (CreateExpenseRequest.SplitEntry entry : request.splits()) {
                User user = userRepository.findById(entry.userId())
                        .orElseThrow(() -> new RuntimeException("User not found: " + entry.userId()));
                expense.getSplits().add(ExpenseSplit.builder()
                        .expense(expense)
                        .user(user)
                        .owedAmount(entry.value())
                        .build());
            }
        } else if (splitType.equals("PERCENTAGE")) {
            BigDecimal sum = request.splits().stream()
                    .map(CreateExpenseRequest.SplitEntry::value)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new RuntimeException("Percentages must sum to 100");
            }
            for (CreateExpenseRequest.SplitEntry entry : request.splits()) {
                User user = userRepository.findById(entry.userId())
                        .orElseThrow(() -> new RuntimeException("User not found: " + entry.userId()));
                BigDecimal owedAmount = request.amount()
                        .multiply(entry.value())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                expense.getSplits().add(ExpenseSplit.builder()
                        .expense(expense)
                        .user(user)
                        .owedAmount(owedAmount)
                        .build());
            }
        } else {
            throw new RuntimeException("Unsupported split type: " + splitType);
        }

        Expense saved = expenseRepository.save(expense);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("expenseDescription", saved.getDescription());
        metadata.put("amount", saved.getAmount());
        metadata.put("splitType", saved.getSplitType().name());
        activityEventService.log(group, paidBy, ActivityEventType.EXPENSE_ADDED, metadata);

        return saved;
    }

    public List<Expense> getExpensesForGroup(UUID groupId) {
        return expenseRepository.findByGroupId(groupId);
    }

    public ParseExpenseResponse parseExpense(String input, UUID groupId, UUID userId) {
        // 1. Look up the user's display name for the payer context
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Call the AI parser
        ExpenseParserService.ExpenseParseResult result = expenseParserService.parse(input, user.getDisplayName());

        // 3. If HIGH confidence and no clarification needed — auto-create the expense
        if ("HIGH".equals(result.confidence()) && !result.needsClarification() && result.amount() != null) {
            CreateExpenseRequest parseRequest = new CreateExpenseRequest(
                    groupId, result.amount(), result.description(),
                    result.currency() != null ? result.currency() : "USD",
                    "EQUAL", List.of(), null);
            Expense expense = createExpense(parseRequest, userId);
            return new ParseExpenseResponse(result, groupId, true, expense.getId());
        }

        // 4. Otherwise, return the parsed result for user confirmation
        return new ParseExpenseResponse(result, groupId, false, null);
    }
}
