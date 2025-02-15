package com.example.springsaas.subscriptionmanagement.dto;

import com.example.springsaas.subscriptionmanagement.entity.Subscription.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {
    
    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan plan;
    
    private String paymentMethodId;
} 