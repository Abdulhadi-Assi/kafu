package com.kafu.kafu.donation;

import com.kafu.kafu.payment.PaymentMethod;
import com.kafu.kafu.problem.Problem;
import com.kafu.kafu.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation")
@Data
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "payment_method", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DonationStatus status;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "donation_date", nullable = false)
    private LocalDateTime donationDate;

    @Column(name = "idempotency_key")
    private String idempotencyKey;
}
