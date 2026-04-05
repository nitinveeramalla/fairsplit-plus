package com.fairsplit.core.repository;

import com.fairsplit.core.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {

    @Query("SELECT es FROM ExpenseSplit es WHERE es.expense.group.id = :groupId")
    List<ExpenseSplit> findByGroupId(@Param("groupId") UUID groupId);
}