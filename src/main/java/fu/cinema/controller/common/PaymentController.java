package fu.cinema.controller.common;

import fu.cinema.config.VNPayConfig;
import fu.cinema.dto.request.PaymentCreateRequest;
import fu.cinema.dto.response.PaymentCallbackResponse;
import fu.cinema.dto.response.PaymentUrlResponse;
import fu.cinema.dto.response.ReviewOrderResponse;
import fu.cinema.enums.PaymentMethod;
import fu.cinema.service.BookingService;
import fu.cinema.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    @GetMapping("/method")
    public String selectPaymentMethod(@RequestParam("bookingId") Long bookingId, Model model, Authentication auth) {
        ReviewOrderResponse data = bookingService.getReviewOrderData(bookingId);
        model.addAttribute("data", data);

        boolean isStaff = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STAFF"));
        model.addAttribute("isStaff", isStaff);

        return "common/payment-selection";
    }

    @PostMapping("/vnpay/create")
    public String createVNPayPayment(@RequestParam("bookingId") Long bookingId, HttpServletRequest request) {
        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .bookingId(bookingId)
                .ipAddress(VNPayConfig.getIpAddress(request)) // Tách IP tại Controller
                .build();

        PaymentUrlResponse response = paymentService.createVNPayUrl(createRequest);
        return "redirect:" + response.getPaymentUrl();
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams, Authentication auth, RedirectAttributes redirectAttributes) {
        // Validation được che giấu toàn bộ dưới Service thông qua DTO
        PaymentCallbackResponse validationResponse = paymentService.validateVNPayCallback(allParams);

        Long bookingId = validationResponse.getBookingId();
        boolean isStaff = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STAFF"));
        String successRedirect = isStaff ? "redirect:/staff/bookings/" + bookingId : "redirect:/customer/bookings/" + bookingId + "/tickets";
        String failRedirect = "redirect:/payment/method?bookingId=" + bookingId;

        if (validationResponse.isSuccess()) {
            paymentService.processSuccessfulPayment(bookingId, PaymentMethod.VNPAY, validationResponse.getTransactionNo());
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán VNPay thành công! Vé điện tử đã sẵn sàng.");
            return successRedirect;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", validationResponse.getMessage());
            return failRedirect;
        }
    }

    @PostMapping("/cash/confirm")
    public String cashPaymentConfirm(@RequestParam("bookingId") Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            paymentService.processSuccessfulPayment(bookingId, PaymentMethod.CASH, null);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thu tiền mặt thành công! Vé và Bắp nước đã được xuất.");
            return "redirect:/staff/bookings/" + bookingId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phát hành vé: " + e.getMessage());
            return "redirect:/payment/method?bookingId=" + bookingId;
        }
    }
}