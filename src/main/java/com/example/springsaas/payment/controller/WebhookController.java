package com.example.springsaas.payment.controller;

import com.example.springsaas.subscriptionmanagement.service.SubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final SubscriptionService subscriptionService;

    @Value("${stripe.api.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;
            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            }

            switch (event.getType()) {
                case "checkout.session.completed":
                    Session session = (Session) stripeObject;
                    subscriptionService.handleCheckoutSessionCompleted(session);
                    break;
                case "customer.subscription.updated":
                    com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                    subscriptionService.handleSubscriptionUpdated(subscription);
                    break;
                case "customer.subscription.deleted":
                    subscription = (com.stripe.model.Subscription) stripeObject;
                    subscriptionService.handleSubscriptionCanceled(subscription);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok().body("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }
} 