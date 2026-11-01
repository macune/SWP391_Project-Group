package fu.cinema.controller.customer;

import fu.cinema.dto.request.ChangePasswordRequest;
import fu.cinema.dto.request.CustomerRequest;
import fu.cinema.dto.response.MovieResponse;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.TicketRepository;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.exception.AppException;
import fu.cinema.service.BookingService;
import fu.cinema.service.CustomerService;
import fu.cinema.service.PromotionService;
import fu.cinema.service.PublicMovieService;
import fu.cinema.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final PromotionService promotionService;
    private final PublicMovieService movieService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final BookingService bookingService;
    private final TicketService ticketService;
    private final CustomerService customerService;

    @GetMapping("/bookings")
    public String myBookings(Authentication authentication, Model model) {
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        if (authentication != null) {
            List<TicketResponse> allTickets = ticketService.getMyTickets(authentication.getName());
            List<TicketResponse> activeTickets = allTickets.stream()
                    .filter(t -> t.getStatus() != TicketStatus.CANCELLED)
                    .collect(Collectors.toList());
            model.addAttribute("tickets", activeTickets);
        }
        return "customer/my-tickets";
    }

    @GetMapping("/tickets/{ticketId}")
    public String viewTicketDetail(@PathVariable("ticketId") Long ticketId,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            TicketResponse ticket = ticketService.getTicketDetail(ticketId, username);

            if (ticket.getStatus() == TicketStatus.LOCKED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vé này chưa được thanh toán. Vui lòng thanh toán để nhận mã QR.");
                return "redirect:/payment/method?bookingId=" + ticket.getBookingId();
            }

            if (ticket.getStatus() == TicketStatus.CANCELLED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vé này đã bị hủy do quá hạn thanh toán.");
                return "redirect:/customer/bookings";
            }

            List<TicketResponse> siblingTickets = ticketService.getTicketsByBooking(ticket.getBookingId(), username);
            List<String> fnbDetails = bookingService.getFnbDetailsByBookingId(ticket.getBookingId());

            model.addAttribute("ticket", ticket);
            model.addAttribute("bookingTickets", siblingTickets);
            model.addAttribute("fnbDetails", fnbDetails);
            model.addAttribute("sectionTitle", "Chi tiết Vé điện tử #" + ticket.getTicketId());
            return "customer/ticket-detail";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    @GetMapping("/bookings/{bookingId}/tickets")
    public String viewBookingTickets(@PathVariable("bookingId") Long bookingId,
                                     Authentication authentication,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            List<TicketResponse> tickets = ticketService.getTicketsByBooking(bookingId, username);
            if (tickets.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đơn đặt vé này chưa có vé điện tử nào.");
                return "redirect:/customer/bookings";
            }
            model.addAttribute("ticket", tickets.get(0));
            model.addAttribute("bookingTickets", tickets);
            model.addAttribute("sectionTitle", "Vé điện tử - Đơn hàng #" + bookingId);
            return "customer/ticket-detail";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    @GetMapping("/promotions")
    public String myPromotions(Model model) {
        model.addAttribute("sectionTitle", "Ưu đãi & Khuyến mãi của tôi");
        model.addAttribute("coupons", promotionService.getActivePromotions());
        return "customer/promotions";
    }

    @GetMapping("/profile")
    public String myProfile(Model model, Authentication authentication) {
        model.addAttribute("sectionTitle", "Hồ sơ cá nhân");
        String username = authentication.getName();

        if (!model.containsAttribute("profileReq")) {
            model.addAttribute("profileReq", customerService.getCustomerProfile(username));
        }
        model.addAttribute("username", username);
        return "customer/profile"; // Trả về file profile.html
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileReq") CustomerRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileReq", bindingResult);
            redirectAttributes.addFlashAttribute("profileReq", request);
            return "redirect:/customer/profile";
        }

        try {
            customerService.updateCustomerProfile(authentication.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/customer/profile";
    }

    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("sectionTitle", "Đổi mật khẩu bảo mật");
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        return "customer/change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                        BindingResult bindingResult,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.changePasswordRequest", bindingResult);
            redirectAttributes.addFlashAttribute("changePasswordRequest", request);
            return "redirect:/customer/change-password";
        }

        try {
            customerService.changePassword(authentication.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/change-password";
    }

    @GetMapping("/dashboard")
    public String customerDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate showDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate bookingDate,
            @RequestParam(required = false) Integer bookingMonth,
            @RequestParam(required = false) Integer bookingYear,
            Model model
    ) {
        model.addAttribute("sectionTitle", "Tổng quan");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        long upcomingTicketCount = ticketRepository.countByBooking_Account_UsernameAndBooking_StatusAndStatusInAndShowtime_StartTimeAfter(
                username, BookingStatus.CONFIRMED, List.of(TicketStatus.BOOKED, TicketStatus.CHECKED_IN), LocalDateTime.now());

        long completedBookingCount = bookingRepository.countByAccount_UsernameAndStatus(username, BookingStatus.CONFIRMED);

        model.addAttribute("upcomingTicketCount", upcomingTicketCount);
        model.addAttribute("completedBookingCount", completedBookingCount);

        LocalDateTime showFrom = null;
        LocalDateTime showTo = null;

        if (month != null && month >= 1 && month <= 12) {
            LocalDateTime monthFrom = LocalDate.of(year != null ? year : LocalDate.now().getYear(), month, 1).atStartOfDay();
            LocalDateTime monthTo = monthFrom.toLocalDate().plusMonths(1).atStartOfDay();
            showFrom = showFrom == null || monthFrom.isAfter(showFrom) ? monthFrom : showFrom;
            showTo = showTo == null || monthTo.isBefore(showTo) ? monthTo : showTo;
        }

        if (year != null && year >= 1900 && year <= 9998) {
            LocalDateTime yearFrom = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime yearTo = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            showFrom = showFrom == null || yearFrom.isAfter(showFrom) ? yearFrom : showFrom;
            showTo = showTo == null || yearTo.isBefore(showTo) ? yearTo : showTo;
        }

        LocalDateTime bookingFrom = null;
        LocalDateTime bookingTo = null;

        if (bookingMonth != null && bookingMonth >= 1 && bookingMonth <= 12) {
            int selectedYear = bookingYear != null ? bookingYear : LocalDate.now().getYear();
            YearMonth selectedMonth = YearMonth.of(selectedYear, bookingMonth);
            LocalDateTime monthFrom = selectedMonth.atDay(1).atStartOfDay();
            LocalDateTime monthTo = selectedMonth.plusMonths(1).atDay(1).atStartOfDay();
            bookingFrom = bookingFrom == null || monthFrom.isAfter(bookingFrom) ? monthFrom : bookingFrom;
            bookingTo = bookingTo == null || monthTo.isBefore(bookingTo) ? monthTo : bookingTo;
        }

        if (bookingYear != null && bookingYear >= 1900 && bookingYear <= 9998) {
            LocalDateTime yearFrom = LocalDate.of(bookingYear, 1, 1).atStartOfDay();
            LocalDateTime yearTo = LocalDate.of(bookingYear + 1, 1, 1).atStartOfDay();
            bookingFrom = bookingFrom == null || yearFrom.isAfter(bookingFrom) ? yearFrom : bookingFrom;
            bookingTo = bookingTo == null || yearTo.isBefore(bookingTo) ? yearTo : bookingTo;
        }

        Page<Ticket> ticketPage = ticketRepository.searchCustomerTickets(
                username, TicketStatus.LOCKED, showFrom, showTo, bookingFrom, bookingTo, PageRequest.of(Math.max(page, 0), 9)
        );

        model.addAttribute("ticketPage", ticketPage);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("bookingMonth", bookingMonth);
        model.addAttribute("bookingYear", bookingYear);

        List<Object[]> topRows = ticketRepository.findTopMoviesByBookingCount(
                BookingStatus.CONFIRMED, List.of(TicketStatus.BOOKED, TicketStatus.CHECKED_IN), MovieStatus.NOW_SHOWING, PageRequest.of(0, 4)
        );

        List<TopMovie> topMovies = topRows.stream()
                .map(row -> new TopMovie(movieService.getMovieDetail((Long) row[0]), ((Number) row[1]).longValue()))
                .toList();

        model.addAttribute("topMovies", topMovies);

        return "customer/dashboard";
    }

    public record TopMovie(MovieResponse movie, long ticketCount) {}
}