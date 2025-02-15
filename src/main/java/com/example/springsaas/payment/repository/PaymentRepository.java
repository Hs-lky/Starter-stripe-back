package com.example.springsaas.payment.repository;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.payment.entity.Payment;
import com.example.springsaas.payment.entity.Payment.PaymentStatus;
import com.example.springsaas.subscriptionmanagement.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByUser(User user);
    List<Payment> findAllByUserAndStatus(User user, PaymentStatus status);
    List<Payment> findAllBySubscription(Subscription subscription);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Payment> findAllByStatusAndCreatedAtBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
} 