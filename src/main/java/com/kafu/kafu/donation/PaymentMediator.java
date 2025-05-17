package com.kafu.kafu.donation;

import com.kafu.kafu.payment.PaymentMethod;
import com.kafu.kafu.payment.PaymentService;
import com.kafu.kafu.payment.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentMediator {
    private final DonationService donationService;

    private final Map<PaymentMethod, PaymentService> paymentServices;

    public PaymentSessionResponse initiateDonation(Long problemId , PaymentSessionRequest request) {

        PaymentService paymentService = paymentServices.get(request.getPaymentMethod());
        if (paymentService == null) {
            throw new RuntimeException("Unsupported payment method");
        }

        PaymentSessionResponse paymentSession = paymentService.createPaymentSession(request);
        DonationDTO dto =
        DonationDTO.builder()
                .problemId(problemId)
                .paymentTransactionId(paymentSession.getSessionId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .isAnonymous(request.getIsAnonymous())
                .idempotencyKey(request.getIdempotencyKey()).build();
        donationService.donate(dto);
        return paymentSession;
    }

    public void handleWebhookEvent(String payload, Map<String, String> headers,PaymentMethod paymentMethod) {
        PaymentService paymentService = paymentServices.get(paymentMethod);
        if (paymentService == null) {
            throw new RuntimeException("Unsupported payment method");
        }
        WebhookEvent webhookEvent=paymentService.getEvent(payload,headers);
        donationService.updateStatus(webhookEvent);
    }




}