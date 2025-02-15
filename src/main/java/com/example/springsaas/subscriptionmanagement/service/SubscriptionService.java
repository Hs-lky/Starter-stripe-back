package com.example.springsaas.subscriptionmanagement.service;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.authentication.repository.UserRepository;
import com.example.springsaas.subscriptionmanagement.dto.SubscriptionRequest;
import com.example.springsaas.subscriptionmanagement.dto.SubscriptionResponse;
import com.example.springsaas.subscriptionmanagement.entity.Subscription;
import com.example.springsaas.subscriptionmanagement.entity.Subscription.SubscriptionStatus;
import com.example.springsaas.subscriptionmanagement.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Price;
import com.stripe.model.SetupIntent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
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
    public SubscriptionResponse createSubscription(SubscriptionRequest request) throws StripeException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Check if user already has an active subscription
        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new RuntimeException("User already has an active subscription");
        }

        // Create or get Stripe customer
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null) {
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", user.getEmail());
            customerParams.put("name", user.getFirstName() + " " + user.getLastName());
            Customer customer = Customer.create(customerParams);
            stripeCustomerId = customer.getId();
            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
        }

        // Attach payment method to customer if provided
        if (request.getPaymentMethodId() != null) {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(request.getPaymentMethodId());
            paymentMethod.attach(Map.of("customer", stripeCustomerId));
            
            // Set as default payment method
            Customer customer = Customer.retrieve(stripeCustomerId);
            customer.update(Map.of("invoice_settings", 
                Map.of("default_payment_method", request.getPaymentMethodId())));
        }

        // Create Stripe subscription
        Map<String, Object> item = new HashMap<>();
        item.put("price", PLAN_PRICE_IDS.get(request.getPlan()));

        Map<String, Object> items = new HashMap<>();
        items.put("0", item);

        Map<String, Object> params = new HashMap<>();
        params.put("customer", stripeCustomerId);
        params.put("items", items);
        params.put("payment_behavior", "default_incomplete");
        params.put("payment_settings", Map.of(
            "payment_method_types", List.of("card"),
            "save_default_payment_method", "on_subscription"
        ));
        if (request.getPaymentMethodId() != null) {
            params.put("default_payment_method", request.getPaymentMethodId());
        }

        com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.create(params);

        // Get price details
        Price price = Price.retrieve(PLAN_PRICE_IDS.get(request.getPlan()));

        // Create local subscription
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(request.getPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAmount(BigDecimal.valueOf(price.getUnitAmount() / 100.0));
        subscription.setCurrency(price.getCurrency().toUpperCase());
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setCurrentPeriodStart(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()),
                ZoneId.systemDefault()
        ));
        subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()),
                ZoneId.systemDefault()
        ));

        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return mapToResponse(savedSubscription);
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long subscriptionId) throws StripeException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to cancel this subscription");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Subscription is not active");
        }

        // Cancel Stripe subscription
        com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
        stripeSubscription.cancel();

        // Update local subscription
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return mapToResponse(savedSubscription);
    }

    public List<SubscriptionResponse> getUserSubscriptions() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return subscriptionRepository.findAllByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

    // Add new method for creating setup intent
    public Map<String, String> createSetupIntent() throws StripeException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Create or get Stripe customer
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null) {
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", user.getEmail());
            customerParams.put("name", user.getFirstName() + " " + user.getLastName());
            Customer customer = Customer.create(customerParams);
            stripeCustomerId = customer.getId();
            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
        }

        // Create Setup Intent
        Map<String, Object> params = new HashMap<>();
        params.put("customer", stripeCustomerId);
        params.put("payment_method_types", List.of("card"));
        SetupIntent setupIntent = SetupIntent.create(params);

        return Map.of(
            "clientSecret", setupIntent.getClientSecret(),
            "customerId", stripeCustomerId
        );
    }

    @Transactional
    public Map<String, String> createCheckoutSession(SubscriptionRequest request) throws StripeException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Create or get Stripe customer
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null) {
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", user.getEmail());
            customerParams.put("name", user.getFirstName() + " " + user.getLastName());
            Customer customer = Customer.create(customerParams);
            stripeCustomerId = customer.getId();
            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
        }

        // Create Checkout Session
        Map<String, Object> params = new HashMap<>();
        params.put("customer", stripeCustomerId);
        params.put("line_items", List.of(Map.of("price", PLAN_PRICE_IDS.get(request.getPlan()), "quantity", 1)));
        params.put("mode", "subscription");
        params.put("success_url", "http://localhost:4200/dashboard/subscription/success?session_id={CHECKOUT_SESSION_ID}");
        params.put("cancel_url", "http://localhost:4200/dashboard/subscription/cancel");

        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);

        return Map.of(
            "sessionId", session.getId(),
            "sessionUrl", session.getUrl()
        );
    }
} 