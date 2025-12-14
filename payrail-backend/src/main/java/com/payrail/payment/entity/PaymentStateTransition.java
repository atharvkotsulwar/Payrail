package com.payrail.payment.entity;

import com.payrail.payment.PaymentStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_state_transitions")
public class PaymentStateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to Payment row
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private PaymentStatus fromStatus;   // can be null for first transition

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private PaymentStatus toStatus;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PaymentStateTransition() {
    }

    // ---- getters & setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public PaymentStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(PaymentStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public PaymentStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(PaymentStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
