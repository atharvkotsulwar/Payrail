package com.payrail.payment.repository;

import com.payrail.payment.entity.StripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StripeEventRepository extends JpaRepository<StripeEvent, Long> {
    Optional<StripeEvent> findByEventId(String eventId);
}
