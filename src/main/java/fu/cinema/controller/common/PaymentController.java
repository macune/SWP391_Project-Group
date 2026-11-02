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
        // Tạo một DTO Request chứa mã đơn hàng và IP của người dùng (VNPay yêu cầu phải có IP)
        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .bookingId(bookingId)
                .ipAddress(VNPayConfig.getIpAddress(request))
                .build();

        // Gọi Service để tính toán chữ ký bảo mật và tạo ra đường dẫn (URL) trỏ sang VNPay
        PaymentUrlResponse response = paymentService.createVNPayUrl(createRequest);

        // Dùng lệnh "redirect:" để ép trình duyệt của người dùng tự động nhảy sang trang của ngân hàng
        return "redirect:" + response.getPaymentUrl();
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams, Authentication auth, RedirectAttributes redirectAttributes) {
        // allParams chứa TẤT CẢ các thông số mà VNPay gửi về trên thanh URL.
        // Chuyển toàn bộ mảng này xuống Service để kiểm tra chữ ký bảo mật (Chống fake kết quả)
        PaymentCallbackResponse validationResponse = paymentService.validateVNPayCallback(allParams);

        Long bookingId = validationResponse.getBookingId();

        // Chuẩn bị sẵn 2 đường dẫn: 1 cho Staff, 1 cho Khách hàng
        boolean isStaff = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STAFF"));
        String successRedirect = isStaff ? "redirect:/staff/bookings/" + bookingId : "redirect:/customer/bookings/" + bookingId + "/tickets";
        String failRedirect = "redirect:/payment/method?bookingId=" + bookingId;

        // Nếu Service xác nhận chữ ký đúng VÀ mã phản hồi là "00" (Thành công)
        if (validationResponse.isSuccess()) {
            // Gọi hàm chốt đơn (Xuất vé, trừ kho, lưu lịch sử)
            paymentService.processSuccessfulPayment(bookingId, PaymentMethod.VNPAY, validationResponse.getTransactionNo());
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán VNPay thành công! Vé điện tử đã sẵn sàng.");
            return successRedirect; // Điều hướng tới trang Xem Vé/Hóa Đơn
        } else {
            // Nếu giao dịch bị hủy hoặc lỗi chữ ký
            redirectAttributes.addFlashAttribute("errorMessage", validationResponse.getMessage());
            return failRedirect; // Đẩy về lại trang Chọn phương thức thanh toán
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