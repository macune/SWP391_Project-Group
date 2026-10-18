package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final BranchStockService branchStockService;
    private final AccountRepository accountRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    @GetMapping("/fnb")
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
    }
    private Account getCurrentStaffAccount() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return accountRepository.findByUsername(auth.getName())
                .orElseThrow(() ->
                        new RuntimeException("Tài khoản không tồn tại"));
    }

}