package com.example.springsaas.subscriptionmanagement.dto;

import com.example.springsaas.subscriptionmanagement.entity.Subscription.SubscriptionPlan;
import com.example.springsaas.subscriptionmanagement.entity.Subscription.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private String stripeSubscriptionId;
    private LocalDateTime canceledAt;
    private String cancelReason;
} 