package com.payrail.payment.service;

import com.payrail.payment.PaymentStatus;
import com.payrail.payment.entity.LedgerEntry;
import com.payrail.payment.entity.Payment;
import com.payrail.payment.repository.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Persist a ledger record.
     * Note: we write ledger entries only when a payment becomes final (SUCCESS/FAILED).
     */
    @Transactional
    public LedgerEntry recordPayment(Payment payment, PaymentStatus status, String source, String description) {
        LedgerEntry entry = new LedgerEntry();
        entry.setStripePaymentId(payment.getStripePaymentId());
        entry.setAmount(payment.getAmount() == null ? 0L : payment.getAmount());
        entry.setCurrency(payment.getCurrency());
        entry.setStatus(status);
        entry.setSource(source == null ? "SYSTEM" : source);
        entry.setDescription(description == null ? "Payment status recorded" : description);
        entry.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt() : Instant.now());
        return ledgerEntryRepository.save(entry);
    }

    // Compatibility helper
    public LedgerEntry recordPayment(Payment payment, PaymentStatus status) {
        return recordPayment(payment, status, "WEBHOOK", "Payment status update recorded");
    }

    public Page<LedgerEntry> getLedgerPage(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ledgerEntryRepository.findAll(pageable);
    }
}
