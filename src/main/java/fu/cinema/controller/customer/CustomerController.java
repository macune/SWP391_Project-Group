package fu.cinema.controller.customer;

import fu.cinema.dto.response.BookingHistoryResponse;
import fu.cinema.enums.BookingStatus;
import fu.cinema.service.BookingService;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.exception.AppException;
import fu.cinema.service.PromotionService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final PromotionService promotionService;
    private final BookingService bookingService;
    private final TicketService ticketService;

    @GetMapping("/dashboard")
    public String customerDashboard(Model model) {
        model.addAttribute("sectionTitle", "Tổng quan");
        return "customer/dashboard";
    }

    @GetMapping("/bookings")
    public String myBookings(Authentication authentication, Model model) {
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        if (authentication != null) {
            List<TicketResponse> allTickets = ticketService.getMyTickets(authentication.getName());

            List<TicketResponse> activeTickets = allTickets.stream()
                    .filter(t -> t.getStatus() != fu.cinema.enums.TicketStatus.CANCELLED)
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

            if (ticket.getStatus() == fu.cinema.enums.TicketStatus.LOCKED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vé này chưa được thanh toán. Vui lòng thanh toán để nhận mã QR.");
                return "redirect:/payment/method?bookingId=" + ticket.getBookingId();
            }

            if (ticket.getStatus() == fu.cinema.enums.TicketStatus.CANCELLED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vé này đã bị hủy do quá hạn thanh toán.");
                return "redirect:/customer/bookings";
            }

            List<TicketResponse> siblingTickets = ticketService.getTicketsByBooking(ticket.getBookingId(), username);

            List<String> fnbDetails = bookingService.getFnbDetailsByBookingId(ticket.getBookingId());

            model.addAttribute("ticket", ticket);
            model.addAttribute("bookingTickets", siblingTickets);
            model.addAttribute("fnbDetails", fnbDetails); // Chuyen danh sach F&B xuong View
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


    @GetMapping("/profile")
    public String myProfile(Model model) {
        model.addAttribute("sectionTitle", "Hồ sơ cá nhân");
        return "customer/dashboard";
    }

    @GetMapping("/promotions")
    public String myPromotions(Model model) {
        model.addAttribute("sectionTitle", "Ưu đãi & Khuyến mãi của tôi");
        model.addAttribute("coupons", promotionService.getActivePromotions());
        return "customer/promotions";
    }

    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("sectionTitle", "Đổi mật khẩu bảo mật");
        return "customer/dashboard";
    }
}