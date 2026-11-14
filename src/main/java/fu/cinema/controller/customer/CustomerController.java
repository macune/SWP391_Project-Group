package fu.cinema.controller.customer;

import fu.cinema.dto.request.ChangePasswordRequest;
import fu.cinema.dto.request.CustomerRequest;
import fu.cinema.dto.response.DashboardResponse;
import fu.cinema.dto.response.MovieResponse;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.TicketStatus;
import fu.cinema.exception.AppException;
import fu.cinema.service.BookingService;
import fu.cinema.service.CustomerService;
import fu.cinema.service.DashboardService;
import fu.cinema.service.PromotionService;
import fu.cinema.service.PublicMovieService;
import fu.cinema.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final PromotionService promotionService;
    private final PublicMovieService movieService;
    private final DashboardService dashboardService;
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
            @RequestParam(required = false) String showDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String bookingDate,
            @RequestParam(required = false) Integer bookingMonth,
            @RequestParam(required = false) Integer bookingYear,
            Model model
    ) {
        model.addAttribute("sectionTitle", "Tổng quan");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        LocalDate parsedShowDate = parseDateFlexible(showDate);
        LocalDate parsedBookingDate = parseDateFlexible(bookingDate);

        DashboardResponse.Customer data = dashboardService.getCustomerDashboardData(
                username, page, 9, parsedShowDate, month, year, parsedBookingDate, bookingMonth, bookingYear
        );

        model.addAttribute("upcomingTicketCount", data.upcomingTicketCount());
        model.addAttribute("completedBookingCount", data.completedBookingCount());
        model.addAttribute("ticketPage", data.ticketPage());
        model.addAttribute("topMovies", data.topMovies());
        model.addAttribute("showDate", data.showDate());
        model.addAttribute("month", data.month());
        model.addAttribute("year", data.year());
        model.addAttribute("bookingDate", data.bookingDate());
        model.addAttribute("bookingMonth", data.bookingMonth());
        model.addAttribute("bookingYear", data.bookingYear());

        return "customer/dashboard";
    }

    private LocalDate parseDateFlexible(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
    }
}