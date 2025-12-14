package com.payrail.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentRequest {

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be at least 1 (in cents)")
    private Long amount;

    @NotBlank(message = "currency is required")
    private String currency;

    public PaymentRequest() {
    }

    public PaymentRequest(Long amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
