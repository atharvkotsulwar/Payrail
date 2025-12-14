package com.payrail.payment.repository;

import com.payrail.payment.entity.Payment;
import com.payrail.payment.entity.PaymentStateTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentStateTransitionRepository extends JpaRepository<PaymentStateTransition, Long> {

    // For future: to fetch history per payment
    List<PaymentStateTransition> findByPaymentOrderByCreatedAtAsc(Payment payment);
}
