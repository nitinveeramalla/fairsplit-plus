package com.fairsplit.api.controller;

import com.fairsplit.api.dto.DebtResponse;
import com.fairsplit.api.dto.RecordSettlementRequest;
import com.fairsplit.api.service.SettlementService;
import com.fairsplit.api.utils.UserUtils;
import com.fairsplit.core.entity.Settlement;
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
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    private final UserUtils userUtils;

    private final UserRepository userRepository;

    public SettlementController(SettlementService settlementService, UserUtils userUtils, UserRepository userRepository) {
        this.settlementService = settlementService;
        this.userUtils = userUtils;
        this.userRepository = userRepository;
    }

    @GetMapping("/group/{groupId}/balances")
    public ResponseEntity<List<DebtResponse>> getAllDebts(@PathVariable UUID groupId) {
        List<DebtResponse> debts = settlementService.getSimplifiedDebts(groupId)
                .stream()
                .map(s -> {
                    String fromName = userRepository.findById(s.from())
                            .map(User::getDisplayName).orElse(s.from().toString());
                    String toName = userRepository.findById(s.to())
                            .map(User::getDisplayName).orElse(s.to().toString());
                    return new DebtResponse(fromName, toName, s.amount());
                })
                .toList();
        return ResponseEntity.ok(debts);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Settlement>> getAllSettlements(@PathVariable UUID groupId) {
        return ResponseEntity.ok(settlementService.getSettlementHistory(groupId));
    }

    @PostMapping
    public ResponseEntity<Settlement> recordSettlement(@RequestBody RecordSettlementRequest request,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userUtils.getUser(userDetails);
        Settlement settlement = settlementService.recordSettlement(
                request.groupId(), user.getId(), request.paidToId(),
                request.amount(), request.note());
        return ResponseEntity.status(201).body(settlement);
    }
}
