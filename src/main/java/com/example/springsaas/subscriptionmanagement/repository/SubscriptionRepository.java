package com.example.springsaas.subscriptionmanagement.repository;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.subscriptionmanagement.entity.Subscription;
import com.example.springsaas.subscriptionmanagement.entity.Subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    List<Subscription> findAllByUser(User user);
    List<Subscription> findAllByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, LocalDateTime date);
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);
} 