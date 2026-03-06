package com.fairsplit.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class DebtSimplificationServiceTest {

    private DebtSimplificationService service;
    private final UUID alice = UUID.randomUUID();
    private final UUID bob   = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final UUID dave  = UUID.randomUUID();

    @BeforeEach
    void setUp() { service = new DebtSimplificationService(); }

    @Test
    @DisplayName("Simple two-person debt produces one settlement")
    void simpleTwoPerson() {
        var result = service.simplify(Map.of(
                alice, new BigDecimal("50.00"),
                bob,   new BigDecimal("-50.00")
        ));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).from()).isEqualTo(bob);
        assertThat(result.get(0).to()).isEqualTo(alice);
        assertThat(result.get(0).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Chain A->B->C simplified to A pays C directly")
    void chainDebt() {
        var result = service.simplify(Map.of(
                alice, new BigDecimal("-10.00"),
                bob,   new BigDecimal("0.00"),
                carol, new BigDecimal("10.00")
        ));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).from()).isEqualTo(alice);
        assertThat(result.get(0).to()).isEqualTo(carol);
    }

    @Test
    @DisplayName("Four-person group uses at most n-1 settlements")
    void fourPerson() {
        var result = service.simplify(Map.of(
                alice, new BigDecimal("30.00"),
                bob,   new BigDecimal("20.00"),
                carol, new BigDecimal("-25.00"),
                dave,  new BigDecimal("-25.00")
        ));
        assertThat(result).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Zero balances produce no settlements")
    void zeroBalances() {
        assertThat(service.simplify(Map.of(alice, BigDecimal.ZERO))).isEmpty();
    }
}