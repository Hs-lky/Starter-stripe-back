package com.example.springsaas.subscriptionmanagement.repository;

import com.example.springsaas.subscriptionmanagement.entity.SubscriptionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionAuditRepository extends JpaRepository<SubscriptionAudit, Long> {
} 