package com.example.springsaas.payment.entity;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.common.entity.BaseEntity;
import com.example.springsaas.subscriptionmanagement.entity.Subscription;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false)
    private String stripePaymentIntentId;

    @Column
    private String stripeChargeId;

    @Column
    private LocalDateTime paidAt;

    @Column
    private String failureReason;

    @Column
    private String receiptUrl;

    public enum PaymentStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        REFUNDED
    }
} 