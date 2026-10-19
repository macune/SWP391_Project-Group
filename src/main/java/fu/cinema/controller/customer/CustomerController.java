package fu.cinema.controller.customer;

import fu.cinema.dto.response.TicketResponse;
import fu.cinema.exception.AppException;
import fu.cinema.service.PromotionService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final PromotionService promotionService;
    private final TicketService ticketService;

    // Trang tổng quan
    @GetMapping("/dashboard")
    public String customerDashboard(Model model) {
        model.addAttribute("sectionTitle", "Tổng quan");
        return "customer/dashboard";
    }

    // Trang Vé của tôi (Danh sách vé điện tử)
    @GetMapping("/bookings")
    public String myBookings(Authentication authentication, Model model) {
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        if (authentication != null) {
            List<TicketResponse> myTickets = ticketService.getMyTickets(authentication.getName());
            model.addAttribute("tickets", myTickets);
        }
        return "customer/my-tickets";
    }

    // Xem chi tiết 1 vé điện tử kèm QR Code
    @GetMapping("/tickets/{ticketId}")
    public String viewTicketDetail(@PathVariable("ticketId") Long ticketId,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            TicketResponse ticket = ticketService.getTicketDetail(ticketId, username);

            // Lấy thêm toàn bộ vé trong cùng đơn đặt vé để người dùng tiện chuyển đổi qua lại
            List<TicketResponse> siblingTickets = ticketService.getTicketsByBooking(ticket.getBookingId(), username);

            model.addAttribute("ticket", ticket);
            model.addAttribute("bookingTickets", siblingTickets);
            model.addAttribute("sectionTitle", "Chi tiết Vé điện tử #" + ticket.getTicketId());
            return "customer/ticket-detail";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    // Xem toàn bộ vé điện tử (QR Code) thuộc 1 đơn đặt vé (Booking)
    // (Được gọi từ nút "View & Download E-Ticket" ở màn hình Booking Confirmation)
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

    // Endpoint kích hoạt / phát hành vé điện tử (QR Code) cho 1 Booking
    @PostMapping("/bookings/{bookingId}/generate-tickets")
    public String generateBookingETickets(@PathVariable("bookingId") Long bookingId,
                                          @RequestParam(value = "showtimeId", required = false) Long showtimeId,
                                          @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                                          RedirectAttributes redirectAttributes) {
        try {
            ticketService.generateETicketsForBooking(bookingId, showtimeId, seatIds);
            redirectAttributes.addFlashAttribute("successMessage", "Khởi tạo vé điện tử và mã QR thành công!");
            return "redirect:/customer/bookings/" + bookingId + "/tickets";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    // Trang Hồ sơ cá nhân
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

    // Trang Đổi mật khẩu
    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("sectionTitle", "Đổi mật khẩu bảo mật");
        return "customer/dashboard";
    }
}