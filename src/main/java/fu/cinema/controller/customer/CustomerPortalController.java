package fu.cinema.controller.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerPortalController {

    // 1. Trang tổng quan tài khoản thành viên
    @GetMapping("/dashboard")
    public String customerDashboard(Model model) {
        model.addAttribute("sectionTitle", "Tổng quan tài khoản");
        return "customer/dashboard";
    }

    // 2. Trang Vé của tôi (Lịch sử đặt vé)
    @GetMapping("/bookings")
    public String myBookings(Model model) {
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        return "customer/dashboard";
    }

    // 3. Trang Hồ sơ cá nhân
    @GetMapping("/profile")
    public String myProfile(Model model) {
        model.addAttribute("sectionTitle", "Hồ sơ cá nhân");
        return "customer/dashboard";
    }

    // 4. Trang Ưu đãi & Khuyến mãi
    @GetMapping("/promotions")
    public String myPromotions(Model model) {
        model.addAttribute("sectionTitle", "Ưu đãi & Khuyến mãi của tôi");
        return "customer/dashboard";
    }

    // 5. Trang Đổi mật khẩu
    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("sectionTitle", "Đổi mật khẩu bảo mật");
        return "customer/dashboard";
    }
}