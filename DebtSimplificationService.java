package com.fairsplit.core.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Debt Simplification Algorithm
 *
 * Reduces the number of transactions needed to settle a group using a
 * greedy min-transactions approach on net balances.
 *
 * Example: A owes B $10, B owes C $10 → simplified to A pays C $10 directly.
 * Reduces O(n²) individual debts to at most O(n-1) transactions.
 */
@Service
public class DebtSimplificationService {

    public record Settlement(UUID from, UUID to, BigDecimal amount) {}

    /**
     * @param balances map of userId → net balance
     *                 positive = is owed money (creditor)
     *                 negative = owes money (debtor)
     * @return minimum list of settlements to zero out all balances
     */
    public List<Settlement> simplify(Map<UUID, BigDecimal> balances) {
        // Filter out zero balances
        List<Map.Entry<UUID, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<UUID, BigDecimal>> debtors = new ArrayList<>();

        for (var entry : balances.entrySet()) {
            BigDecimal bal = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(Map.entry(entry.getKey(), bal));
            } else if (bal.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(Map.entry(entry.getKey(), bal.abs()));
            }
        }

        // Use priority queues to always match largest debtor with largest creditor
        PriorityQueue<Map.Entry<UUID, BigDecimal>> creditorQueue =
                new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));
        PriorityQueue<Map.Entry<UUID, BigDecimal>> debtorQueue =
                new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));

        creditorQueue.addAll(creditors);
        debtorQueue.addAll(debtors);

        List<Settlement> settlements = new ArrayList<>();

        while (!creditorQueue.isEmpty() && !debtorQueue.isEmpty()) {
            var creditor = creditorQueue.poll();
            var debtor = debtorQueue.poll();

            BigDecimal settlementAmount = creditor.getValue().min(debtor.getValue());
            settlements.add(new Settlement(debtor.getKey(), creditor.getKey(), settlementAmount));

            BigDecimal creditorRemainder = creditor.getValue().subtract(settlementAmount);
            BigDecimal debtorRemainder = debtor.getValue().subtract(settlementAmount);

            if (creditorRemainder.compareTo(BigDecimal.valueOf(0.01)) > 0) {
                creditorQueue.offer(Map.entry(creditor.getKey(), creditorRemainder));
            }
            if (debtorRemainder.compareTo(BigDecimal.valueOf(0.01)) > 0) {
                debtorQueue.offer(Map.entry(debtor.getKey(), debtorRemainder));
            }
        }

        return settlements;
    }
}
