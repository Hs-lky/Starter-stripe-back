package com.example.springsaas.subscriptionmanagement.entity;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "subscription_audits")
public class SubscriptionAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Subscription.SubscriptionPlan plan;

    @Column(nullable = false)
    private String stripeSessionId;

    @Column(nullable = false)
    private String status;

    @Column
    private String errorMessage;
} 