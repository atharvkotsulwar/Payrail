package com.payrail.payment.repository;

import com.payrail.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripePaymentId(String stripePaymentId);

}
