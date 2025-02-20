package com.example.springsaas.subscriptionmanagement.service;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.authentication.repository.UserRepository;
import com.example.springsaas.subscriptionmanagement.dto.SubscriptionRequest;
import com.example.springsaas.subscriptionmanagement.dto.SubscriptionResponse;
import com.example.springsaas.subscriptionmanagement.entity.Subscription;
import com.example.springsaas.subscriptionmanagement.entity.SubscriptionAudit;
import com.example.springsaas.subscriptionmanagement.entity.Subscription.SubscriptionStatus;
import com.example.springsaas.subscriptionmanagement.repository.SubscriptionAuditRepository;
import com.example.springsaas.subscriptionmanagement.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; 
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAuditRepository auditRepository;
    private final UserRepository userRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private final Map<Subscription.SubscriptionPlan, String> PLAN_PRICE_IDS = Map.of(
            Subscription.SubscriptionPlan.BASIC, "price_1QsqFcPTJe3xQo0CAz3WS0Eo",
            Subscription.SubscriptionPlan.PREMIUM, "price_premium_monthly",
            Subscription.SubscriptionPlan.ENTERPRISE, "price_enterprise_monthly"
    );

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public Map<String, String> createCheckoutSession(SubscriptionRequest request) throws StripeException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Creating checkout session for user: {} with plan: {}", user.getEmail(), request.getPlan());

        try {
            // Validate request
            validateSubscriptionRequest(user, request);

            // Create or get Stripe customer
            String stripeCustomerId = getOrCreateStripeCustomer(user);

            // Get price details for the subscription
            Price price = Price.retrieve(PLAN_PRICE_IDS.get(request.getPlan()));

            // Create Checkout Session
            Map<String, Object> params = new HashMap<>();
            params.put("customer", stripeCustomerId);
            params.put("line_items", List.of(Map.of("price", PLAN_PRICE_IDS.get(request.getPlan()), "quantity", 1)));
            params.put("mode", "subscription");
            params.put("success_url", "http://localhost:4200/dashboard/subscription/success?session_id={CHECKOUT_SESSION_ID}");
            params.put("cancel_url", "http://localhost:4200/dashboard/subscription/cancel");

            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
            log.info("Checkout session created successfully for user: {} with sessionId: {}", user.getEmail(), session.getId());

            // Create initial subscription record (PENDING status)
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(request.getPlan());
            subscription.setStatus(SubscriptionStatus.PENDING);
            subscription.setAmount(BigDecimal.valueOf(price.getUnitAmount() / 100.0));
            subscription.setCurrency(price.getCurrency().toUpperCase());
            subscription.setStripeCustomerId(stripeCustomerId);
            
            // Set initial period dates
            LocalDateTime now = LocalDateTime.now();
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(now.plusMonths(1));
            
            // Save the subscription first to ensure it exists
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            log.info("Created initial subscription record with id: {} for user: {}", savedSubscription.getId(), user.getEmail());

            // Create audit record
            createAuditRecord(user, request.getPlan(), session.getId(), "CHECKOUT_CREATED", null);

            return Map.of(
                "sessionId", session.getId(),
                "sessionUrl", session.getUrl()
            );
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("Failed to create checkout session for user: {} with plan: {}", user.getEmail(), request.getPlan(), e);
            
            // Create audit record for failure
            createAuditRecord(user, request.getPlan(), null, "CHECKOUT_FAILED", errorMessage);
            
            throw e;
        }
    }

    public List<SubscriptionResponse> getUserSubscriptions() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return subscriptionRepository.findAllByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SubscriptionResponse> getUserSubscriptionsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return subscriptionRepository.findAllByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SubscriptionResponse getUserActiveSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .map(this::mapToResponse)
                .orElse(null);
    }

    private void validateSubscriptionRequest(User user, SubscriptionRequest request) {
        if (!PLAN_PRICE_IDS.containsKey(request.getPlan())) {
            log.error("Invalid subscription plan requested: {}", request.getPlan());
            throw new IllegalArgumentException("Invalid subscription plan");
        }

        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            log.error("User {} already has an active subscription", user.getEmail());
            throw new IllegalStateException("User already has an active subscription");
        }

        if (!user.isEnabled()) {
            log.error("User {} is not enabled", user.getEmail());
            throw new IllegalStateException("User account is not enabled");
        }
    }

    private String getOrCreateStripeCustomer(User user) throws StripeException {
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null) {
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", user.getEmail());
            customerParams.put("name", user.getFirstName() + " " + user.getLastName());
            Customer customer = Customer.create(customerParams);
            stripeCustomerId = customer.getId();
            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
            log.info("Created new Stripe customer for user: {}", user.getEmail());
        }
        return stripeCustomerId;
    }

    private void createAuditRecord(User user, Subscription.SubscriptionPlan plan, String sessionId, String status, String errorMessage) {
        SubscriptionAudit audit = new SubscriptionAudit();
        audit.setUser(user);
        audit.setPlan(plan);
        audit.setStripeSessionId(sessionId);
        audit.setStatus(status);
        audit.setErrorMessage(errorMessage);
        auditRepository.save(audit);
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .plan(subscription.getPlan())
                .status(subscription.getStatus())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .stripeSubscriptionId(subscription.getStripeSubscriptionId())
                .canceledAt(subscription.getCanceledAt())
                .cancelReason(subscription.getCancelReason())
                .build();
    }

    @Transactional
    public void handleCheckoutSessionCompleted(com.stripe.model.checkout.Session session) {
        log.info("Handling checkout session completed: {}", session.getId());
        String stripeCustomerId = session.getCustomer();
        String stripeSubscriptionId = session.getSubscription();
        
        log.debug("Looking up user for stripeCustomerId: {}", stripeCustomerId);
        User user = userRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> {
                    log.error("User not found for customer: {}", stripeCustomerId);
                    return new RuntimeException("User not found for customer: " + stripeCustomerId);
                });
        
        log.debug("Looking up pending subscription for user: {}", user.getEmail());
        Subscription subscription = subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.PENDING)
                .orElseThrow(() -> {
                    log.error("No pending subscription found for user: {}", user.getEmail());
                    return new RuntimeException("No pending subscription found for user: " + user.getEmail());
                });
        
        try {
            log.debug("Retrieving Stripe subscription: {}", stripeSubscriptionId);
            com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setCurrentPeriodStart(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), 
                    ZoneId.systemDefault()));
            subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), 
                    ZoneId.systemDefault()));
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            log.info("Subscription activated: {} for user: {}", savedSubscription.getId(), user.getEmail());
            
            // Create audit record for successful subscription
            createAuditRecord(user, subscription.getPlan(), session.getId(), "SUBSCRIPTION_ACTIVATED", null);
            
        } catch (StripeException e) {
            String errorMessage = "Error retrieving Stripe subscription: " + e.getMessage();
            log.error(errorMessage, e);
            createAuditRecord(user, subscription.getPlan(), session.getId(), "ACTIVATION_FAILED", errorMessage);
            throw new RuntimeException("Error activating subscription", e);
        }
    }

    @Transactional
    public void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        log.info("Handling subscription update: {}", stripeSubscription.getId());
        
        Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseThrow(() -> {
                    log.error("Subscription not found: {}", stripeSubscription.getId());
                    return new RuntimeException("Subscription not found: " + stripeSubscription.getId());
                });
        
        subscription.setCurrentPeriodStart(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), 
                ZoneId.systemDefault()));
        subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), 
                ZoneId.systemDefault()));
        
        String stripeStatus = stripeSubscription.getStatus();
        log.debug("Stripe subscription status: {}", stripeStatus);
        
        if ("past_due".equals(stripeStatus)) {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
        } else if ("unpaid".equals(stripeStatus)) {
            subscription.setStatus(SubscriptionStatus.UNPAID);
        } else if ("active".equals(stripeStatus)) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscription updated: {} with status: {}", savedSubscription.getId(), savedSubscription.getStatus());
    }

    @Transactional
    public void handleSubscriptionCanceled(com.stripe.model.Subscription stripeSubscription) {
        Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + stripeSubscription.getId()));
        
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        subscription.setCancelReason("Canceled via Stripe");
        
        subscriptionRepository.save(subscription);
        log.info("Subscription canceled: {}", subscription.getId());
    }

    @Transactional
    public void activateSubscription(String sessionId) throws StripeException {
        log.info("Activating subscription for session: {}", sessionId);
        
        // Retrieve the session from Stripe
        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.retrieve(sessionId);
        
        if (!"complete".equals(session.getStatus())) {
            throw new RuntimeException("Checkout session is not complete");
        }
        
        String stripeCustomerId = session.getCustomer();
        String stripeSubscriptionId = session.getSubscription();
        
        // Find the user
        User user = userRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> new RuntimeException("User not found for customer: " + stripeCustomerId));
        
        // Find pending subscription
        Subscription subscription = subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending subscription found for user: " + user.getEmail()));
        
        // Retrieve subscription details from Stripe
        com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        
        // Update subscription status
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setCurrentPeriodStart(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), 
                ZoneId.systemDefault()));
        subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), 
                ZoneId.systemDefault()));
        
        subscriptionRepository.save(subscription);
        log.info("Subscription activated: {} for user: {}", subscription.getId(), user.getEmail());
        
        // Create audit record
        createAuditRecord(user, subscription.getPlan(), sessionId, "SUBSCRIPTION_ACTIVATED", null);
    }

    @Transactional
    public Map<String, String> createCustomerPortalSession() throws StripeException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Creating customer portal session for user: {}", user.getEmail());

        try {
            // Ensure user has a Stripe customer ID
            String stripeCustomerId = getOrCreateStripeCustomer(user);

            Map<String, Object> params = new HashMap<>();
            params.put("customer", stripeCustomerId);
            params.put("return_url", "http://localhost:4200/dashboard/subscriptions");

            com.stripe.model.billingportal.Session portalSession = 
                com.stripe.model.billingportal.Session.create(params);

            log.info("Customer portal session created successfully for user: {}", user.getEmail());

            return Map.of(
                "portalUrl", portalSession.getUrl()
            );

        } catch (Exception e) {
            log.error("Failed to create customer portal session for user: {}", user.getEmail(), e);
            throw e;
        }
    }
} 