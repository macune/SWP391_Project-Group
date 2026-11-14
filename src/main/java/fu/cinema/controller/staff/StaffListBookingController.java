package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.entity.Account;
import fu.cinema.enums.BookingStatus;
import fu.cinema.repository.AccountRepository;
import fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffListBookingController {

    private final BookingService bookingService;
    private final AccountRepository accountRepository;

    private Account getCurrentStaffAccount(Authentication auth) {
        return accountRepository.findByUsername(auth.getName()).orElseThrow();
    }

    // 1. DANH SÁCH ĐƠN ĐẶT VÉ
    @GetMapping("/bookings")
    public String showStaffBookings(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) BookingStatus status,
                                    Model model) {
        List<BookDetailsResponse> bookingList = bookingService.getAllBookings(keyword, status);
        model.addAttribute("bookings", bookingList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "staff/booking-list";
    }

    // 2. CHI TIẾT ĐƠN ĐẶT VÉ
    @GetMapping("/bookings/{id}")
    public String showBookingDetails(@PathVariable Long id, Model model) {
        try {
            BookDetailsResponse booking = bookingService.getBookingDetail(id);
            List<String> fnbDetails = bookingService.getFnbDetailsByBookingId(id);
            model.addAttribute("booking", booking);
            model.addAttribute("fnbDetails", fnbDetails);
            return "staff/booking-details";
        } catch (Exception e) {
            return "redirect:/staff/bookings";
        }
    }

    // 3. XỬ LÝ XÓA ĐƠN RÁC (CHỜ THANH TOÁN)
    @PostMapping("/bookings/{id}/hard-delete")
    public String deletePendingBooking(@PathVariable("id") Long bookingId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            Long branchId = getCurrentStaffAccount(auth).getStaff().getBranch().getBranchId();
            bookingService.hardDeletePendingBooking(bookingId, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn đơn hàng rác (chờ thanh toán)!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings";
    }

    // 4. XỬ LÝ HỦY ĐƠN ĐÃ XÁC NHẬN
    @PostMapping("/bookings/{id}/cancel-confirmed")
    public String cancelConfirmedBooking(@PathVariable("id") Long bookingId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            Long branchId = getCurrentStaffAccount(auth).getStaff().getBranch().getBranchId();
            bookingService.cancelConfirmedBooking(bookingId, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn thành công! Đã giải phóng ghế, trừ doanh thu và hoàn bắp nước vào kho.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings";
    }
}
