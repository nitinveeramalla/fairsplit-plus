package com.fairsplit.api.service;

import com.fairsplit.core.entity.Group;
import com.fairsplit.core.entity.Settlement;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.repository.ExpenseRepository;
import com.fairsplit.core.repository.ExpenseSplitRepository;
import com.fairsplit.core.repository.GroupRepository;
import com.fairsplit.core.repository.SettlementRepository;
import com.fairsplit.core.repository.UserRepository;
import com.fairsplit.core.service.DebtSimplificationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SettlementService {

    private final ExpenseRepository expenseRepository;

    private final ExpenseSplitRepository expenseSplitRepository;

    private final SettlementRepository settlementRepository;

    private final DebtSimplificationService debtSimplificationService;

    private final UserRepository userRepository;

    private final GroupRepository groupRepository;


    public SettlementService(ExpenseRepository expenseRepository, ExpenseSplitRepository expenseSplitRepository,
                             SettlementRepository settlementRepository,
                             DebtSimplificationService debtSimplificationService,
                             UserRepository userRepository, GroupRepository groupRepository) {
        this.expenseRepository = expenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.settlementRepository = settlementRepository;
        this.debtSimplificationService = debtSimplificationService;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public Map<UUID, BigDecimal> calculateBalances(UUID groupId) {
        Map<UUID, BigDecimal> balances = new HashMap<>();

        // Each paidBy user gets +amount
        expenseRepository.findByGroupId(groupId).forEach(expense -> {
            UUID paidBy = expense.getPaidBy().getId();
            balances.merge(paidBy, expense.getAmount(), BigDecimal::add);
        });

        // Each split user gets -owedAmount
        expenseSplitRepository.findByGroupId(groupId).forEach(split -> {
            UUID userId = split.getUser().getId();
            balances.merge(userId, split.getOwedAmount().negate(), BigDecimal::add);
        });

        return balances;
    }

    public List<DebtSimplificationService.Settlement> getSimplifiedDebts(UUID groupId) {
        return debtSimplificationService.simplify(calculateBalances(groupId));
    }

    public Settlement recordSettlement(UUID groupId, UUID paidById, UUID paidToId, BigDecimal amount, String note) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User paidBy = userRepository.findById(paidById)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User paidTo = userRepository.findById(paidToId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Settlement settlement = Settlement.builder()
                .group(group)
                .paidBy(paidBy)
                .paidTo(paidTo)
                .amount(amount)
                .note(note)
                .build();

        return settlementRepository.save(settlement);
    }

    public List<Settlement> getSettlementHistory(UUID groupId) {
        return settlementRepository.findByGroupId(groupId);
    }
}
