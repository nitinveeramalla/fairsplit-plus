package com.fairsplit.core.repository;

import com.fairsplit.core.entity.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    List<ActivityEvent> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}