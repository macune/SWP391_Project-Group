package fu.cinema.controller.common;

import fu.cinema.dto.request.SeatLockRequest;
import fu.cinema.dto.response.ReviewOrderResponse;
import fu.cinema.dto.response.SeatSelectionResponse;
import fu.cinema.service.BookingService;
import fu.cinema.service.PromotionService;
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
    PromotionService promotionService;

    @GetMapping("/seat-selection")
    public String showSeatSelection(@RequestParam("showtimeId") Long showtimeId, Model model) {
        try {
            SeatSelectionResponse response = bookingService.getSeatSelectionData(showtimeId);
            model.addAttribute("data", response);
            return "common/seat-selection";
        } catch (Exception e) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaff = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_MANAGER"));
            return isStaff ? "redirect:/staff/movies/today" : "redirect:/customer/list-movies";
        }
    }

    @PostMapping("/lock-seats")
    public String lockSeats(@ModelAttribute SeatLockRequest request, RedirectAttributes redirectAttributes) {
        try {
            Long bookingId = bookingService.lockSeats(request);
            return "redirect:/booking/review?bookingId=" + bookingId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/seat-selection?showtimeId=" + request.getShowtimeId();
        }
    }

    @GetMapping("/route-to-fnb")
    public String routeToFnB(@RequestParam("bookingId") Long bookingId, Authentication auth) {
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        return isStaff ? "redirect:/staff/booking/" + bookingId + "/fnb"
                : "redirect:/customer/booking/" + bookingId + "/fnb";
    }

    @GetMapping("/review")
    public String showReviewOrder(@RequestParam("bookingId") Long bookingId, Model model) {
        try {
            ReviewOrderResponse reviewOrderResponse = bookingService.getReviewOrderData(bookingId);
            model.addAttribute("data", reviewOrderResponse);

            // BƠM DANH SÁCH MÃ KHUYẾN MÃI NẾU NGƯỜI DÙNG LÀ NHÂN VIÊN/QUẢN LÝ
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaffOrManager = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_MANAGER"));

            if (isStaffOrManager) {
                model.addAttribute("activePromotions", promotionService.getActivePromotions());
            }

            return "common/review-order";
        } catch (Exception e) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaff = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_MANAGER"));
            return isStaff ? "redirect:/staff/movies/today" : "redirect:/customer/list-movies";
        }
    }

    @PostMapping("/apply-promotion")
    public String applyPromotion(@RequestParam("bookingId") Long bookingId, @RequestParam("promoCode") String promoCode, RedirectAttributes redirectAttributes) {
        try {
            bookingService.applyPromotion(bookingId, promoCode);
            redirectAttributes.addFlashAttribute("successMessage", "Áp dụng mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/booking/review?bookingId=" + bookingId;
    }

    @PostMapping("/cancel")
    public String cancelBooking(@RequestParam("bookingId") Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_MANAGER"));
        return isStaff ? "redirect:/staff/movies/today" : "redirect:/customer/list-movies";
    }

    @PostMapping("/confirm")
    public String confirmBooking(@RequestParam("bookingId") Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.confirmBooking(bookingId);
            return "common/payment-testing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/review?bookingId=" + bookingId;
        }
    }
}