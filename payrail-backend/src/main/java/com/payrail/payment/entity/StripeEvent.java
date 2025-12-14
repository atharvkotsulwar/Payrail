package com.payrail.payment.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stripe_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stripe_event_id", columnNames = "event_id")
})
public class StripeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    public StripeEvent() {}

    public StripeEvent(String eventId, String type, Instant receivedAt) {
        this.eventId = eventId;
        this.type = type;
        this.receivedAt = receivedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
