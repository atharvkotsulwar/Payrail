package com.payrail.payment.repository;

import com.payrail.payment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByStripePaymentIdOrderByCreatedAtDesc(String stripePaymentId);
}
