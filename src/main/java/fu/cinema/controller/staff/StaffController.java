package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.enums.BookingStatus;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AccountRepository accountRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService; // GỌI SERVICE ĐÃ CÓ SẴN CỦA BẠN

    @GetMapping("/fnb")
    public String fnbBookingPage(Authentication auth) {
        Account staffAcc = accountRepository.findByUsername(auth.getName()).orElseThrow();
        Booking retailBooking = Booking.builder()
                .account(staffAcc)
                .bookingTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .build();
        bookingRepository.save(retailBooking);
        return "redirect:/staff/booking/" + retailBooking.getBookingId() + "/fnb";
    }

    // 1. HIỂN THỊ DANH SÁCH & LỌC TÌM KIẾM
    @GetMapping("/bookings")
    public String showStaffBookings(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) BookingStatus status,
                                    Model model) {

        // Dùng đúng hàm getAllBookings bạn đã viết trong Service
        List<BookDetailsResponse> bookingList = bookingService.getAllBookings(keyword, status);

        model.addAttribute("bookings", bookingList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "staff/booking-list";
    }

    // 2. HIỂN THỊ CHI TIẾT ĐƠN HÀNG
    @GetMapping("/bookings/{id}")
    public String showBookingDetails(@PathVariable Long id, Model model) {
        try {
            // Dùng đúng hàm getBookingDetail bạn đã viết
            BookDetailsResponse booking = bookingService.getBookingDetail(id);
            model.addAttribute("booking", booking);
            return "staff/booking-details";
        } catch (Exception e) {
            return "redirect:/staff/bookings";
        }
    }

    // 3. CẬP NHẬT TRẠNG THÁI
    @PostMapping("/bookings/{id}/edit")
    public String updateBookingStatus(@PathVariable Long id,
                                      @RequestParam BookingStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            // Dùng đúng hàm updateBookingStatus bạn đã viết
            bookingService.updateBookingStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            // Bắt exception (ví dụ lỗi đổi trạng thái khi đơn đã Cancelled)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings/" + id;
    }
}