package com.fairsplit.api.controller;

import com.fairsplit.api.dto.CreateExpenseRequest;
import com.fairsplit.api.service.ExpenseService;
import com.fairsplit.core.entity.Expense;
import com.fairsplit.core.entity.Group;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    private final UserRepository userRepository;

    public ExpenseController(ExpenseService expenseService, UserRepository userRepository) {
        this.expenseService = expenseService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody CreateExpenseRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Expense newExpense = expenseService.createExpense(request.groupId(), user.getId(), request.amount(),
                request.description(), request.currency());
        return ResponseEntity.status(201).body(newExpense);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Expense>> getAllExpenses(@PathVariable UUID groupId) {
        return ResponseEntity.ok(expenseService.getExpensesForGroup(groupId));
    }
}
