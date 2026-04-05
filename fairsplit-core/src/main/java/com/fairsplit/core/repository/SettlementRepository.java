package com.fairsplit.core.repository;

import com.fairsplit.core.entity.Settlement;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    List<Settlement> findByGroupId(UUID groupId);

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId AND (s.paidBy.id = :userId OR s.paidTo.id = :userId)")
    List<Settlement> findByGroupAndUser(@Param("groupId") UUID groupId, @Param("userId") UUID userId);
}
