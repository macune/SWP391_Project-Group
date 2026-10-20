package fu.cinema.service;

import fu.cinema.dto.request.PaymentCreateRequest;
import fu.cinema.dto.response.PaymentCallbackResponse;
import fu.cinema.dto.response.PaymentUrlResponse;
import fu.cinema.enums.PaymentMethod;

import java.util.Map;

public interface PaymentService {
    PaymentUrlResponse createVNPayUrl(PaymentCreateRequest request);
    PaymentCallbackResponse validateVNPayCallback(Map<String, String> params);
    void processSuccessfulPayment(Long bookingId, PaymentMethod method, String transactionNo);
}