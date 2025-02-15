package com.example.springsaas.subscriptionmanagement.controller;

import com.example.springsaas.subscriptionmanagement.dto.SubscriptionRequest;
import com.example.springsaas.subscriptionmanagement.dto.SubscriptionResponse;
import com.example.springsaas.subscriptionmanagement.service.SubscriptionService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) throws StripeException {
        return ResponseEntity.ok(subscriptionService.createSubscription(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable Long id) throws StripeException {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(id));
    }

    @PostMapping("/setup-intent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> createSetupIntent() throws StripeException {
        return ResponseEntity.ok(subscriptionService.createSetupIntent());
    }

    @PostMapping("/create-checkout-session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @Valid @RequestBody SubscriptionRequest request) throws StripeException {
        return ResponseEntity.ok(subscriptionService.createCheckoutSession(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SubscriptionResponse>> getUserSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions());
    }
} 