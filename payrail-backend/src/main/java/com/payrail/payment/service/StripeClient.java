package com.payrail.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class StripeClient {

    @Value("${stripe.secretKey:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank() || secretKey.startsWith("sk_test_your_")) {
            throw new IllegalStateException(
                    "stripe.secretKey is missing/placeholder. Set stripe.secretKey=sk_test_... in application.properties"
            );
        }
        Stripe.apiKey = secretKey.trim();
    }

    /**
     * Create + confirm a PaymentIntent using a card token (tok_visa).
     * Fixes "return_url required" by forcing allow_redirects=never.
     * Fixes "only one of automatic_payment_methods, confirmation_method" by NOT sending confirmation_method.
     */
    public PaymentIntent createAndConfirmPaymentIntent(String token, Long amountInCents, String currency)
            throws StripeException {

        if (amountInCents == null || amountInCents <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value in cents");
        }

        if (currency == null || currency.isBlank()) currency = "usd";
        currency = currency.trim().toLowerCase();

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Stripe token header 'token' is required (e.g. tok_visa)");
        }

        // payment_method_data: { type: "card", card: { token: "tok_visa" } }
        Map<String, Object> card = new HashMap<>();
        card.put("token", token);

        Map<String, Object> paymentMethodData = new HashMap<>();
        paymentMethodData.put("type", "card");
        paymentMethodData.put("card", card);

        // automatic_payment_methods: enable but NEVER allow redirects -> avoids return_url requirement
        Map<String, Object> automaticPaymentMethods = new HashMap<>();
        automaticPaymentMethods.put("enabled", true);
        automaticPaymentMethods.put("allow_redirects", "never");

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amountInCents);
        params.put("currency", currency);
        params.put("payment_method_data", paymentMethodData);
        params.put("confirm", true);
        params.put("automatic_payment_methods", automaticPaymentMethods);
        params.put("description", "PayRail test PaymentIntent");

        return PaymentIntent.create(params);
    }
}
