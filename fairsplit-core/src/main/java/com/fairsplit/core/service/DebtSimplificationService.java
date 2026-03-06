package com.fairsplit.core.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DebtSimplificationService {

    public record Settlement(UUID from, UUID to, BigDecimal amount) {}

    /**
     * Reduces the number of transactions needed to settle a group.
     * @param balances map of userId → net balance
     *                 positive = is owed money (creditor)
     *                 negative = owes money (debtor)
     * @return minimum list of settlements to zero out all balances
     */
    public List<Settlement> simplify(Map<UUID, BigDecimal> balances) {
        PriorityQueue<Map.Entry<UUID, BigDecimal>> creditors =
                new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));
        PriorityQueue<Map.Entry<UUID, BigDecimal>> debtors =
                new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));

        for (var entry : balances.entrySet()) {
            BigDecimal bal = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                creditors.offer(Map.entry(entry.getKey(), bal));
            } else if (bal.compareTo(BigDecimal.ZERO) < 0) {
                debtors.offer(Map.entry(entry.getKey(), bal.abs()));
            }
        }

        List<Settlement> settlements = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var creditor = creditors.poll();
            var debtor   = debtors.poll();

            BigDecimal amount = creditor.getValue().min(debtor.getValue());
            settlements.add(new Settlement(debtor.getKey(), creditor.getKey(), amount));

            BigDecimal creditorRemainder = creditor.getValue().subtract(amount);
            BigDecimal debtorRemainder   = debtor.getValue().subtract(amount);

            if (creditorRemainder.compareTo(new BigDecimal("0.01")) > 0) {
                creditors.offer(Map.entry(creditor.getKey(), creditorRemainder));
            }
            if (debtorRemainder.compareTo(new BigDecimal("0.01")) > 0) {
                debtors.offer(Map.entry(debtor.getKey(), debtorRemainder));
            }
        }

        return settlements;
    }
}