package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.dto.response.TicketValidationResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.exception.AppException;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.BookingService;
import fu.cinema.service.BranchStockService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final ShowtimeRepository showtimeRepository;
    private final BookingService bookingService;
    private final TicketService ticketService;
    private final BranchStockService branchStockService;

    private Account getCurrentStaffAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return accountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Account staffAcc = getCurrentStaffAccount();
        Long branchId = staffAcc.getStaff().getBranch().getBranchId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<Booking> confirmedBookings = bookingRepository.findByStatusAndBookingTimeBetween(
                BookingStatus.CONFIRMED, startOfDay, endOfDay);

        BigDecimal dailyRevenue = confirmedBookings.stream()
                .filter(b -> {
                    if (b.getTickets() != null && !b.getTickets().isEmpty()) {
                        return b.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId().equals(branchId);
                    } else if (b.getAccount() != null && b.getAccount().getStaff() != null) {
                        return b.getAccount().getStaff().getBranch().getBranchId().equals(branchId);
                    }
                    return false;
                })
                .map(Booking::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("dailyRevenue", dailyRevenue);

        List<Showtime> todayShowtimes = showtimeRepository
                .findByHall_Branch_BranchIdAndStartTimeBetweenAndStatusNotOrderByStartTimeAsc(
                        branchId, startOfDay, endOfDay, ShowtimeStatus.CANCELLED);

        var menuItems = branchStockService.getStockByBranch(branchId);
        long availableItems = menuItems.stream()
                .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()))
                .count();

        model.addAttribute("branchName", staffAcc.getStaff().getBranch().getBranchName());
        model.addAttribute("today", today);
        model.addAttribute("todayShowtimesCount", todayShowtimes.size());
        model.addAttribute("availableFnbItemsCount", availableItems);
        model.addAttribute("upcomingShowtimes", todayShowtimes.stream()
                .filter(st -> !st.getStartTime().isBefore(LocalDateTime.now()))
                .limit(5)
                .toList());

        return "staff/dashboard";
    }

    @GetMapping("/fnb")
    public String fnbBookingPage(Authentication auth) {
        Account staffAcc = accountRepository.findByUsername(auth.getName()).orElseThrow();

        List<Booking> staffBookings = bookingRepository.findByAccount_AccountIdOrderByCreatedAtDesc(staffAcc.getAccountId());

       Booking retailBooking = staffBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING && (b.getTickets() == null || b.getTickets().isEmpty()))
                .findFirst()
                .orElse(null);

        if (retailBooking == null) {
            retailBooking = Booking.builder()
                    .account(staffAcc)
                    .bookingTime(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .totalAmount(BigDecimal.ZERO)
                    .status(BookingStatus.PENDING)
                    .build();
            bookingRepository.save(retailBooking);
        }

        return "redirect:/staff/booking/" + retailBooking.getBookingId() + "/fnb";
    }

}