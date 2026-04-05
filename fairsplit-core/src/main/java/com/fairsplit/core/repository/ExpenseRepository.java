package com.fairsplit.core.repository;

import com.fairsplit.core.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository  extends JpaRepository<Expense, UUID> {

    List<Expense> findByGroupId(UUID groupId);

}
