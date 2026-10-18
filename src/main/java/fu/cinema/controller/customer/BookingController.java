package fu.cinema.controller.customer;

import fu.cinema.dto.request.SeatLockRequest;
import fu.cinema.dto.response.ReviewOrderResponse;
import fu.cinema.dto.response.SeatSelectionResponse;
import fu.cinema.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/booking")
public class BookingController {
    BookingService bookingService;

    // Sơ đồ ghế
    @GetMapping("/seat-selection")
    public String showSeatSelection(@RequestParam("showtimeId") Long showtimeId,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            SeatSelectionResponse response = bookingService.getSeatSelectionData(showtimeId);
            model.addAttribute("data", response);
            return "customer/seat-selection";
        } catch (Exception e) {
            // Kiểm tra: Nếu là STAFF thì quay về Lịch chiếu hôm nay của Staff, nếu là CUSTOMER thì về list-movies
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaff = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
            return isStaff ? "redirect:/staff/movies/today" : "redirect:/customer/list-movies";
        }
    }

    // Khóa ghế tạm thời khi người dùng chọn
    @PostMapping("/lock-seats")
    public String lockSeats(
            @ModelAttribute SeatLockRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            Long bookingId = bookingService.lockSeats(request);

            // Thành công -> Chuyển hướng sang bước chọn bắp nước
            return "redirect:/booking/review?bookingId=" + bookingId;

        } catch (Exception e) {
            // Lỗi -> Quay lại trang chọn ghế và báo lỗi màu đỏ
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/seat-selection?showtimeId=" + request.getShowtimeId();
        }
    }

    // Chi tiết đơn hàng
    @GetMapping("/review")
    public String showReviewOrder(@RequestParam("bookingId") Long bookingId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            ReviewOrderResponse reviewOrderResponse = bookingService.getReviewOrderData(bookingId);
            model.addAttribute("data", reviewOrderResponse);
            return "customer/review-order";
        } catch (Exception e) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaff = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
            return isStaff ? "redirect:/staff/movies/today" : "redirect:/customer/list-movies";
        }
    }

    // Áp dụng mã khuyến mãi (promotion)
    @PostMapping("/apply-promotion")
    public String applyPromotion(@RequestParam("bookingId") Long bookingId,
                                 @RequestParam("promoCode") String promoCode,
                                 RedirectAttributes redirectAttributes) {
        try {
            bookingService.applyPromotion(bookingId, promoCode);
            redirectAttributes.addFlashAttribute("successMessage", "Áp dụng mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/booking/review?bookingId=" + bookingId;
    }

    // Hủy đơn
    @PostMapping("/cancel")
    public String cancelBooking(@RequestParam("bookingId") Long bookingId,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn hàng bookingId = {}: ", bookingId, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        return isStaff ? "redirect:/staff/movies/today" : "redirect:/customer/list-movies";
    }

    // Xác nhận đặt vé -> Payment
    @PostMapping("/confirm")
    public String confirmBooking(@RequestParam("bookingId") Long bookingId,
                                 RedirectAttributes redirectAttributes) {
        try {
            bookingService.confirmBooking(bookingId);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaff = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

            return isStaff ? "common/payment-testing" : "common/payment-testing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/review?bookingId=" + bookingId;
        }
    }
}
