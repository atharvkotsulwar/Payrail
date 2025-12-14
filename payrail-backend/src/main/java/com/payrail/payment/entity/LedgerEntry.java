package com.payrail.payment.entity;

import com.payrail.payment.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Ledger entry persisted to DB.
 *
 * Why DB and not in-memory?
 * - A ledger is an audit log; it must survive restarts.
 * - This fixes the "ledger empty" issue after refresh/restart.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_stripe_payment_id", columnList = "stripe_payment_id"),
        @Index(name = "idx_ledger_created_at", columnList = "created_at")
})
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_payment_id", nullable = false)
    private String stripePaymentId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStripePaymentId() { return stripePaymentId; }
    public void setStripePaymentId(String stripePaymentId) { this.stripePaymentId = stripePaymentId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
