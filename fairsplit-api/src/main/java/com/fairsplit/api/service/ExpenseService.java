package com.fairsplit.api.service;

import com.fairsplit.core.entity.Expense;
import com.fairsplit.core.entity.ExpenseSplit;
import com.fairsplit.core.entity.Group;
import com.fairsplit.core.entity.GroupMember;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.repository.ExpenseRepository;
import com.fairsplit.core.repository.GroupRepository;
import com.fairsplit.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final GroupRepository groupRepository;

    private final ExpenseRepository expenseRepository;

    private final UserRepository userRepository;

    public ExpenseService(GroupRepository groupRepository, ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public Expense createExpense(UUID groupId, UUID paidById, BigDecimal amount, String description, String currency) {
        // 1. Find group and paidBy user
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User paidBy = userRepository.findById(paidById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Calculate equal split amount
        BigDecimal splitAmount = amount.divide(
                BigDecimal.valueOf(group.getMembers().size()), 2, RoundingMode.HALF_UP);

        // 3. Build the expense
        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidBy)
                .createdBy(paidBy)
                .amount(amount)
                .description(description)
                .currency(currency)
                .build();

        // 4. Create a split for each member
        for (GroupMember member : group.getMembers()) {
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(expense)
                    .user(member.getUser())
                    .owedAmount(splitAmount)
                    .build();
            expense.getSplits().add(split);
        }

        // 5. Save and return
        return expenseRepository.save(expense);
    }

    public List<Expense> getExpensesForGroup(UUID groupId) {
        return expenseRepository.findByGroupId(groupId);
    }
}
