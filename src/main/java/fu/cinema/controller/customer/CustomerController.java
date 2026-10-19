package fu.cinema.controller.customer;

import fu.cinema.dto.response.BookingHistoryResponse;
import fu.cinema.enums.BookingStatus;
import fu.cinema.service.BookingService;
import fu.cinema.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final PromotionService promotionService;
    private final BookingService bookingService;

    //Trang tổng quan
    @GetMapping("/dashboard")
    public String customerDashboard(Model model) {
        model.addAttribute("sectionTitle", "Tổng quan");
        return "customer/dashboard";
    }

    // Trang Vé của tôi
    @GetMapping("/bookings")
    public String myBookings(Model model,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) BookingStatus status) {

        List<BookingHistoryResponse> bookings = bookingService.getMyBookings(keyword, status);
        model.addAttribute("bookings", bookings);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        return "customer/my-bookings";
    }

    //Trang Hồ sơ cá nhân
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