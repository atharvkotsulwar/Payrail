package com.payrail.payment;

public enum PaymentStatus {
    CREATED,       // we created a charge request
    PENDING,       // waiting for confirmation (not used much with Charges, but good for future)
    SUCCESS,       // payment completed
    FAILED,        // payment failed
    REFUNDED       // we'll use this later when we add refunds
}
