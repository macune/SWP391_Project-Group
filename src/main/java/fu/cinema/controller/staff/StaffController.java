package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.dto.response.TicketValidationResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.enums.BookingStatus;
import fu.cinema.entity.Branch;
import fu.cinema.exception.AppException;
import fu.cinema.entity.Booking;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.BookingService;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.BranchStockService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AccountRepository accountRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final TicketService ticketService;

    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
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

    @GetMapping("/bookings")
    public String showStaffBookings(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) BookingStatus status,
                                    Model model) {
        List<BookDetailsResponse> bookingList = bookingService.getAllBookings(keyword, status);
        model.addAttribute("bookings", bookingList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "staff/booking-list";
    public String fnbBookingPage(Model model) {
        Account staffAcc = getCurrentStaffAccount();
        Long branchId = staffAcc.getStaff().getBranch().getBranchId();

        model.addAttribute(
                "menuItems",
                branchStockService.getStockByBranch(branchId)
        );

        return "staff/fnb-booking";
    }

    @GetMapping("/check-in")
    public String checkInPage() {
        return "redirect:/staff/dashboard";
    }
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Account staffAcc = getCurrentStaffAccount();
        Long branchId = staffAcc.getStaff().getBranch().getBranchId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);
        List<Booking> confirmedBookings =
                bookingRepository.findByStatusAndBookingTimeBetween(
                        BookingStatus.CONFIRMED,
                        startOfDay,
                        endOfDay
                );

        BigDecimal dailyRevenue = confirmedBookings.stream()
                .map(Booking::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("dailyRevenue", dailyRevenue);
        List<Showtime> todayShowtimes =
                showtimeRepository
                        .findByHall_Branch_BranchIdAndStartTimeBetweenAndStatusNotOrderByStartTimeAsc(
                                branchId,
                                startOfDay,
                                endOfDay,
                                ShowtimeStatus.CANCELLED
                        );

        var menuItems = branchStockService.getStockByBranch(branchId);

        long availableItems = menuItems.stream()
                .filter(item -> item.getQuantity() != null
                        && item.getQuantity() > 0)
                .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()))
                .count();

        model.addAttribute("branchName",
                staffAcc.getStaff().getBranch().getBranchName());
        model.addAttribute("today", today);
        model.addAttribute("todayShowtimesCount", todayShowtimes.size());
        model.addAttribute("availableFnbItemsCount", availableItems);
        model.addAttribute("upcomingShowtimes",
                todayShowtimes.stream()
                        .filter(st -> !st.getStartTime().isBefore(LocalDateTime.now()))
                        .limit(5)
                        .toList());

        return "staff/dashboard";
    public String checkInPage(Authentication authentication, Model model) {
        if (authentication != null) {
            accountRepository.findByUsername(authentication.getName()).ifPresent(acc -> {
                if (acc.getStaff() != null && acc.getStaff().getBranch() != null) {
                    Branch currentBranch = acc.getStaff().getBranch();
                    model.addAttribute("currentBranch", currentBranch);
                }
            });
        }
        return "staff/ticket-checkin";
    }

    @GetMapping("/api/tickets/validate")
    @ResponseBody
    public ResponseEntity<TicketValidationResponse> validateQrTicket(
            @RequestParam("qrCode") String qrCode,
            Authentication authentication) {
        String staffUsername = authentication != null ? authentication.getName() : null;
        TicketValidationResponse result = ticketService.validateTicketByQr(qrCode, staffUsername);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/tickets/{ticketId}/check-in")
    @ResponseBody
    public ResponseEntity<TicketValidationResponse> confirmCheckIn(
            @PathVariable("ticketId") Long ticketId,
            Authentication authentication) {
        try {
            String staffUsername = authentication != null ? authentication.getName() : null;
            TicketValidationResponse result = ticketService.confirmTicketCheckIn(ticketId, staffUsername);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(
                    TicketValidationResponse.builder()
                            .validForCheckIn(false)
                            .validationState("ERROR")
                            .message(e.getMessage())
                            .build()
            );
        }
    } // ĐÃ SỬA: Đóng dấu ngoặc nhọn kết thúc hàm tại đây

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

    @PostMapping("/bookings/{id}/edit")
    public String updateBookingStatus(@PathVariable Long id,
                                      @RequestParam BookingStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            bookingService.updateBookingStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings/" + id;
    }
    private Account getCurrentStaffAccount() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return accountRepository.findByUsername(auth.getName())
                .orElseThrow(() ->
                        new RuntimeException("Tài khoản không tồn tại"));
    }

}