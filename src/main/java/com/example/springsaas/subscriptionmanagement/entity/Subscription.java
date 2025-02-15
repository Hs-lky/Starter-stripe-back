package com.example.springsaas.subscriptionmanagement.entity;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.common.entity.BaseEntity;
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
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column
    private String stripeSubscriptionId;

    @Column
    private String stripeCustomerId;

    @Column
    private LocalDateTime canceledAt;

    @Column
    private String cancelReason;

    public enum SubscriptionPlan {
        FREE,
        BASIC,
        PREMIUM,
        ENTERPRISE
    }

    public enum SubscriptionStatus {
        PENDING("PENDING"),
        ACTIVE("ACTIVE"),
        CANCELED("CANCELED"),
        PAST_DUE("PAST_DUE"),
        UNPAID("UNPAID"),
        TRIAL("TRIAL");

        private final String value;

        SubscriptionStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
} 