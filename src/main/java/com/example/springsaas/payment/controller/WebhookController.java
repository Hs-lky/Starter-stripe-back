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
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final SubscriptionService subscriptionService;

    @Value("${stripe.api.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Processing Stripe webhook event: {} [{}]", event.getType(), event.getId());
            
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (!dataObjectDeserializer.getObject().isPresent()) {
                log.error("Cannot deserialize webhook event data for event: {} [{}]", event.getType(), event.getId());
                return ResponseEntity.badRequest().body("Cannot deserialize webhook event data");
            }
            
            StripeObject stripeObject = dataObjectDeserializer.getObject().get();
            log.debug("Deserialized stripe object class: {}", stripeObject.getClass().getName());

            try {
                switch (event.getType()) {
                    case "checkout.session.completed":
                        if (stripeObject instanceof Session) {
                            Session session = (Session) stripeObject;
                            log.info("Processing checkout session completed: {} for customer: {}", 
                                    session.getId(), session.getCustomer());
                            subscriptionService.handleCheckoutSessionCompleted(session);
                        } else {
                            log.error("Expected Session object but got: {}", stripeObject.getClass().getName());
                        }
                        break;
                        
                    case "customer.subscription.created":
                    case "customer.subscription.updated":
                        if (stripeObject instanceof com.stripe.model.Subscription) {
                            com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                            log.info("Processing subscription update: {} for customer: {}", 
                                    subscription.getId(), subscription.getCustomer());
                            subscriptionService.handleSubscriptionUpdated(subscription);
                        } else {
                            log.error("Expected Subscription object but got: {}", stripeObject.getClass().getName());
                        }
                        break;
                        
                    case "customer.subscription.deleted":
                        if (stripeObject instanceof com.stripe.model.Subscription) {
                            com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                            log.info("Processing subscription deletion: {} for customer: {}", 
                                    subscription.getId(), subscription.getCustomer());
                            subscriptionService.handleSubscriptionCanceled(subscription);
                        } else {
                            log.error("Expected Subscription object but got: {}", stripeObject.getClass().getName());
                        }
                        break;
                        
                    case "invoice.payment_succeeded":
                    case "invoice.paid":
                        log.info("Payment successful for invoice: {} [{}]", event.getId(), stripeObject.getClass().getName());
                        break;
                        
                    case "invoice.payment_failed":
                        log.warn("Payment failed for invoice: {} [{}]", event.getId(), stripeObject.getClass().getName());
                        break;
                        
                    default:
                        log.info("Unhandled event type: {} [{}]", event.getType(), event.getId());
                }

                return ResponseEntity.ok().body("Webhook processed successfully");
            } catch (Exception e) {
                log.error("Error processing webhook event: {} [{}]: {}", event.getType(), event.getId(), e.getMessage(), e);
                return ResponseEntity.badRequest().body("Error processing webhook: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error constructing webhook event: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }
} 