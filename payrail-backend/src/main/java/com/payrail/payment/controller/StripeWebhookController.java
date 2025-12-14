package com.payrail.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payrail.payment.*;
import com.payrail.payment.entity.Payment;
import com.payrail.payment.entity.PaymentStateTransition;
import com.payrail.payment.entity.StripeEvent;
import com.payrail.payment.repository.PaymentRepository;
import com.payrail.payment.repository.PaymentStateTransitionRepository;
import com.payrail.payment.repository.StripeEventRepository;
import com.payrail.payment.service.LedgerService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;
    private final PaymentStateTransitionRepository transitionRepository;
    private final StripeEventRepository stripeEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeWebhookController(PaymentRepository paymentRepository,
                                   LedgerService ledgerService,
                                   PaymentStateTransitionRepository transitionRepository,
                                   StripeEventRepository stripeEventRepository) {
        this.paymentRepository = paymentRepository;
        this.ledgerService = ledgerService;
        this.transitionRepository = transitionRepository;
        this.stripeEventRepository = stripeEventRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
            @RequestBody String payload
    ) {
        boolean devMode = (webhookSecret == null
                || webhookSecret.isBlank()
                || webhookSecret.startsWith("whsec_your_"));

        if (devMode) {
            log.warn("stripe.webhookSecret not set/placeholder — DEV mode (no signature verification)");
            return handleDevWebhook(payload);
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.warn("Failed to parse Stripe event payload", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String eventId = event.getId();
        if (isDuplicateEvent(eventId, event.getType())) {
            log.info("Duplicate webhook event {} ignored", eventId);
            return ResponseEntity.ok("Already processed");
        }

        log.info("Processing Stripe event: type={} id={}", event.getType(), eventId);

        switch (event.getType()) {

            // PaymentIntent final events
            case "payment_intent.succeeded" ->
                    handlePaymentIntentEvent(event, PaymentStatus.SUCCESS, "Stripe webhook: payment_intent.succeeded");

            case "payment_intent.payment_failed" ->
                    handlePaymentIntentEvent(event, PaymentStatus.FAILED, "Stripe webhook: payment_intent.payment_failed");

            // Charge events
            case "charge.succeeded" ->
                    handleChargeEvent(event, PaymentStatus.SUCCESS, "Stripe webhook: charge.succeeded");

            case "charge.failed" ->
                    handleChargeEvent(event, PaymentStatus.FAILED, "Stripe webhook: charge.failed");

            // charge.updated can become final when paid=true & status=succeeded
            case "charge.updated" ->
                    handleChargeUpdated(event);

            default ->
                    log.info("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("Received");
    }

    // DB-backed idempotency
    private boolean isDuplicateEvent(String eventId, String type) {
        try {
            stripeEventRepository.save(new StripeEvent(eventId, type, Instant.now()));
            return false;
        } catch (DataIntegrityViolationException e) {
            return true;
        }
    }

    private void handleChargeUpdated(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        var objOpt = deserializer.getObject();
        if (objOpt.isPresent() && objOpt.get() instanceof Charge charge) {
            boolean paid = Boolean.TRUE.equals(charge.getPaid());
            String status = (charge.getStatus() == null) ? "" : charge.getStatus();

            if (paid && "succeeded".equalsIgnoreCase(status)) {
                handleChargeLike(charge, PaymentStatus.SUCCESS, "Stripe webhook: charge.updated (paid+succeeded)");
            } else if ("failed".equalsIgnoreCase(status)) {
                handleChargeLike(charge, PaymentStatus.FAILED, "Stripe webhook: charge.updated (failed)");
            } else {
                log.info("charge.updated ignored (not final). chargeId={} status={} paid={}",
                        charge.getId(), charge.getStatus(), paid);
            }
            return;
        }

        // RAW JSON fallback
        JsonNode obj = rawObjectJson(deserializer);
        if (obj == null) {
            log.warn("charge.updated: data.object not deserialized and rawJson missing. eventId={}", event.getId());
            return;
        }

        String chargeId = obj.path("id").asText(null);
        String paymentIntentId = obj.path("payment_intent").asText(null);
        boolean paid = obj.path("paid").asBoolean(false);
        String status = obj.path("status").asText("");

        if (paid && "succeeded".equalsIgnoreCase(status)) {
            upsertPaymentFinalFromChargeJson(obj, PaymentStatus.SUCCESS, "Stripe webhook: charge.updated (paid+succeeded)");
        } else if ("failed".equalsIgnoreCase(status)) {
            upsertPaymentFinalFromChargeJson(obj, PaymentStatus.FAILED, "Stripe webhook: charge.updated (failed)");
        } else {
            log.info("charge.updated ignored (not final). chargeId={} status={} paid={}",
                    chargeId, status, paid);
        }
    }

    private void handleChargeEvent(Event event, PaymentStatus toStatus, String reason) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        var objOpt = deserializer.getObject();
        if (objOpt.isPresent() && objOpt.get() instanceof Charge charge) {
            handleChargeLike(charge, toStatus, reason);
            return;
        }

        // ✅ RAW JSON fallback (this is what you were missing)
        JsonNode obj = rawObjectJson(deserializer);
        if (obj == null) {
            log.warn("charge event: data.object not deserialized and rawJson missing. type={} id={}",
                    event.getType(), event.getId());
            return;
        }

        upsertPaymentFinalFromChargeJson(obj, toStatus, reason);
    }

    private void upsertPaymentFinalFromChargeJson(JsonNode chargeObj, PaymentStatus toStatus, String reason) {
        String chargeId = chargeObj.path("id").asText(null);
        String paymentIntentId = chargeObj.path("payment_intent").asText(null);

        String stripePaymentId = (paymentIntentId != null && !paymentIntentId.isBlank())
                ? paymentIntentId
                : chargeId;

        if (stripePaymentId == null || stripePaymentId.isBlank()) return;

        long amount = chargeObj.path("amount").asLong(0);
        String currency = chargeObj.path("currency").asText("usd").toLowerCase();

        upsertFinal(stripePaymentId, amount, currency, toStatus, reason);
    }

    private void handleChargeLike(Charge charge, PaymentStatus toStatus, String reason) {
        String paymentIntentId = charge.getPaymentIntent(); // pi_...
        String chargeId = charge.getId();                   // ch_...

        String stripePaymentId = (paymentIntentId != null && !paymentIntentId.isBlank())
                ? paymentIntentId
                : chargeId;

        log.info("Charge webhook mapping: chargeId={} paymentIntentId={} -> stripePaymentId={}",
                chargeId, paymentIntentId, stripePaymentId);

        long amount = (charge.getAmount() == null) ? 0L : charge.getAmount();
        String currency = (charge.getCurrency() == null) ? "usd" : charge.getCurrency().toLowerCase();

        upsertFinal(stripePaymentId, amount, currency, toStatus, reason);
    }

    private void handlePaymentIntentEvent(Event event, PaymentStatus toStatus, String reason) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        var objOpt = deserializer.getObject();
        if (objOpt.isPresent() && objOpt.get() instanceof PaymentIntent pi) {
            String stripePaymentId = pi.getId(); // pi_...
            long amount = (pi.getAmount() == null) ? 0L : pi.getAmount();
            String currency = (pi.getCurrency() == null) ? "usd" : pi.getCurrency().toLowerCase();

            upsertFinal(stripePaymentId, amount, currency, toStatus, reason);
            return;
        }

        // RAW JSON fallback
        JsonNode obj = rawObjectJson(deserializer);
        if (obj == null) {
            log.warn("PI event: data.object not deserialized and rawJson missing. type={} id={}",
                    event.getType(), event.getId());
            return;
        }

        String stripePaymentId = obj.path("id").asText(null);
        if (stripePaymentId == null || stripePaymentId.isBlank()) return;

        long amount = obj.path("amount").asLong(0);
        String currency = obj.path("currency").asText("usd").toLowerCase();

        upsertFinal(stripePaymentId, amount, currency, toStatus, reason);
    }

    private void upsertFinal(String stripePaymentId,
                             long amount,
                             String currency,
                             PaymentStatus toStatus,
                             String reason) {

        Payment payment = paymentRepository
                .findByStripePaymentId(stripePaymentId)
                .orElseGet(Payment::new);

        PaymentStatus fromStatus = payment.getStatus();

        payment.setStripePaymentId(stripePaymentId);
        payment.setAmount(amount);
        payment.setCurrency(currency);

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(Instant.now());
        }
        payment.setUpdatedAt(Instant.now());

        PaymentStatus effectiveTo = coerceMonotonic(fromStatus, toStatus);
        payment.setStatus(effectiveTo);

        Payment saved = paymentRepository.save(payment);

        if (fromStatus != effectiveTo) {
            PaymentStateTransition transition = new PaymentStateTransition();
            transition.setPayment(saved);
            transition.setFromStatus(fromStatus);
            transition.setToStatus(effectiveTo);
            transition.setReason(reason);
            transition.setCreatedAt(Instant.now());
            transitionRepository.save(transition);

            ledgerService.recordPayment(saved, effectiveTo, "WEBHOOK", reason);
        }

        log.info("Webhook updated payment {}: {} -> {} ({})",
                stripePaymentId, fromStatus, effectiveTo, reason);
    }

    private PaymentStatus coerceMonotonic(PaymentStatus current, PaymentStatus next) {
        if (current == PaymentStatus.SUCCESS || current == PaymentStatus.FAILED) return current;
        if (next == null) return current != null ? current : PaymentStatus.PENDING;
        return next;
    }

    private JsonNode rawObjectJson(EventDataObjectDeserializer deserializer) {
        try {
            String raw = deserializer.getRawJson();
            if (raw == null || raw.isBlank()) return null;
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    // DEV-only webhook path
    private ResponseEntity<String> handleDevWebhook(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            String eventId = root.path("id").asText(null);
            String type = root.path("type").asText(null);
            if (eventId == null || type == null) return ResponseEntity.badRequest().body("Invalid payload");

            if (isDuplicateEvent(eventId, type)) return ResponseEntity.ok("Already processed (dev)");

            JsonNode objNode = root.path("data").path("object");
            if (objNode.isMissingNode() || !objNode.isObject()) return ResponseEntity.badRequest().body("Invalid payload");

            String stripePaymentId = objNode.path("payment_intent").asText(null);
            if (stripePaymentId == null || stripePaymentId.isBlank()) {
                stripePaymentId = objNode.path("id").asText(null);
            }
            if (stripePaymentId == null || stripePaymentId.isBlank()) return ResponseEntity.badRequest().body("Invalid payload");

            long amount = objNode.path("amount").asLong(0);
            String currency = objNode.path("currency").asText("usd").toLowerCase();

            PaymentStatus toStatus =
                    (type.contains("succeeded")) ? PaymentStatus.SUCCESS :
                            (type.contains("failed")) ? PaymentStatus.FAILED :
                                    PaymentStatus.PENDING;

            upsertFinal(stripePaymentId, amount, currency, toStatus, "DEV webhook: " + type);
            return ResponseEntity.ok("Received (dev)");

        } catch (Exception e) {
            log.warn("Failed to parse DEV webhook payload", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }
}
