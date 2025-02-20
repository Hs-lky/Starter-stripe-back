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

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<List<SubscriptionResponse>> getUserSubscriptionsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptionsByUserId(userId));
    }

    @GetMapping("/active/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<SubscriptionResponse> getUserActiveSubscription(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserActiveSubscription(userId));
    }

    @PostMapping("/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> activateSubscription(@RequestParam String sessionId) {
        try {
            subscriptionService.activateSubscription(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
         //   log.error("Failed to activate subscription", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create-portal-session")
    public ResponseEntity<Map<String, String>> createPortalSession() throws StripeException {
        Map<String, String> portalSession = subscriptionService.createCustomerPortalSession();
        return ResponseEntity.ok(portalSession);
    }
}