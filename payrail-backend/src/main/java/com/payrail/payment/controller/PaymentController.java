package com.payrail.payment.controller;

import com.payrail.payment.*;
import com.payrail.payment.dto.PaymentRequest;
import com.payrail.payment.dto.PaymentResponse;
import com.payrail.payment.entity.LedgerEntry;
import com.payrail.payment.entity.Payment;
import com.payrail.payment.exception.PaymentNotFoundException;
import com.payrail.payment.repository.PaymentRepository;
import com.payrail.payment.service.LedgerService;
import com.payrail.payment.service.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final StripeClient stripeClient;
    private final LedgerService ledgerService;
    private final PaymentRepository paymentRepository;

    public PaymentController(StripeClient stripeClient,
                             LedgerService ledgerService,
                             PaymentRepository paymentRepository) {
        this.stripeClient = stripeClient;
        this.ledgerService = ledgerService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/charge")
    public ResponseEntity<PaymentResponse> chargeCard(
            @RequestHeader("token") String token,
            @RequestBody PaymentRequest request
    ) throws Exception {

        Long amountInCents = request.getAmount();
        if (amountInCents == null || amountInCents <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value in cents");
        }

        String rawCurrency = request.getCurrency();
        String currency;
        if (rawCurrency == null || rawCurrency.isBlank()) {
            currency = "usd";
        } else {
            currency = rawCurrency.trim().toLowerCase();
            Set<String> allowed = Set.of("usd", "inr", "eur");
            if (!allowed.contains(currency)) {
                throw new IllegalArgumentException("Unsupported currency: " + rawCurrency);
            }
        }

        try {
            PaymentIntent pi = stripeClient.createAndConfirmPaymentIntent(token, amountInCents, currency);

            String stripePaymentId = pi.getId();

            Payment payment = paymentRepository
                    .findByStripePaymentId(stripePaymentId)
                    .orElseGet(Payment::new);

            PaymentStatus previous = payment.getStatus();

            payment.setStripePaymentId(stripePaymentId);
            payment.setAmount(amountInCents);
            payment.setCurrency(currency);

            PaymentStatus desired = inferStatusFromPaymentIntent(pi);
            PaymentStatus effective = coerceMonotonic(previous, desired);
            payment.setStatus(effective);

            if (payment.getCreatedAt() == null) {
                if (pi.getCreated() != null) payment.setCreatedAt(Instant.ofEpochSecond(pi.getCreated()));
                else payment.setCreatedAt(Instant.now());
            }

            payment.setUpdatedAt(Instant.now());
            payment = paymentRepository.save(payment);

            if (previous != effective && (effective == PaymentStatus.SUCCESS || effective == PaymentStatus.FAILED)) {
                ledgerService.recordPayment(payment, effective, "API", "PaymentIntent confirmed synchronously");
            }

            return ResponseEntity.ok(toResponse(payment));

        } catch (CardException ce) {
            // ✅ Persist FAILED payment if Stripe provides a PaymentIntent id
            String paymentIntentId = null;
            try {
                if (ce.getStripeError() != null && ce.getStripeError().getPaymentIntent() != null) {
                    paymentIntentId = ce.getStripeError().getPaymentIntent().getId();
                }
            } catch (Exception ignore) {}

            if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                Payment payment = paymentRepository
                        .findByStripePaymentId(paymentIntentId)
                        .orElseGet(Payment::new);

                PaymentStatus from = payment.getStatus();

                payment.setStripePaymentId(paymentIntentId);
                payment.setAmount(amountInCents);
                payment.setCurrency(currency);
                if (payment.getCreatedAt() == null) payment.setCreatedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());

                PaymentStatus effective = coerceMonotonic(from, PaymentStatus.FAILED);
                payment.setStatus(effective);

                Payment saved = paymentRepository.save(payment);

                if (from != effective) {
                    ledgerService.recordPayment(saved, effective, "API", "Stripe declined: " + ce.getCode());
                }
            } else {
                // If Stripe doesn't give us a PaymentIntent id (can happen for certain test tokens),
                // still persist a FAILED record so History/Ledger reflect the attempt.
                String syntheticId = "fail_" + UUID.randomUUID();

                Payment payment = new Payment();
                payment.setStripePaymentId(syntheticId);
                payment.setAmount(amountInCents);
                payment.setCurrency(currency);
                payment.setStatus(PaymentStatus.FAILED);
                payment.setCreatedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());

                Payment saved = paymentRepository.save(payment);
                ledgerService.recordPayment(saved, PaymentStatus.FAILED, "API", "Stripe declined: " + ce.getCode());
            }

            throw ce; // keep 400 response behavior
        }
    }

    private PaymentStatus inferStatusFromPaymentIntent(PaymentIntent pi) {
        String s = (pi == null || pi.getStatus() == null) ? "" : pi.getStatus().toLowerCase();
        return switch (s) {
            case "succeeded" -> PaymentStatus.SUCCESS;
            case "canceled" -> PaymentStatus.FAILED;
            case "requires_payment_method" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private PaymentStatus coerceMonotonic(PaymentStatus current, PaymentStatus next) {
        if (current == PaymentStatus.SUCCESS || current == PaymentStatus.FAILED) return current;
        if (next == null) return current != null ? current : PaymentStatus.PENDING;
        return next;
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerEntry>> getLedger(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ledgerService.getLedgerPage(page, size).getContent());
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 10;

        // ✅ newest first (so UI always shows latest on top)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentRepository.findAll(pageable);

        List<PaymentResponse> body = paymentsPage
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentByStripeId(
            @PathVariable("paymentId") String stripePaymentId
    ) {
        Payment payment = paymentRepository
                .findByStripePaymentId(stripePaymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException("Payment with id '%s' not found"
                                .formatted(stripePaymentId)));

        return ResponseEntity.ok(toResponse(payment));
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getStripePaymentId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
