package com.kafu.kafu.donation;

import com.kafu.kafu.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationDTO {
    private Long id;

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    private Long donorId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "USD";

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String paymentTransactionId;

    private DonationStatus status;

    private Boolean isAnonymous = false;

    private LocalDateTime donationDate;

    private String idempotencyKey;
}
