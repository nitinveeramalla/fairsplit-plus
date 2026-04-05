package com.fairsplit.api.controller;

import com.fairsplit.api.dto.CreateExpenseRequest;
import com.fairsplit.api.dto.ParseExpenseRequest;
import com.fairsplit.api.dto.ParseExpenseResponse;
import com.fairsplit.api.service.ExpenseService;
import com.fairsplit.api.utils.UserUtils;
import com.fairsplit.core.entity.Expense;
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


    private final UserUtils userUtils;

    public ExpenseController(ExpenseService expenseService, UserUtils userUtils) {
        this.expenseService = expenseService;
        this.userUtils = userUtils;
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody CreateExpenseRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userUtils.getUser(userDetails);
        Expense newExpense = expenseService.createExpense(request.groupId(), user.getId(), request.amount(),
                request.description(), request.currency());
        return ResponseEntity.status(201).body(newExpense);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Expense>> getAllExpenses(@PathVariable UUID groupId) {
        return ResponseEntity.ok(expenseService.getExpensesForGroup(groupId));
    }

    @PostMapping("/parse")
    public ResponseEntity<ParseExpenseResponse> parseExpense(@RequestBody ParseExpenseRequest request,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = userUtils.getUser(userDetails);
        return ResponseEntity.ok(expenseService.parseExpense(request.input(), request.groupId(), user.getId()));
    }
}
