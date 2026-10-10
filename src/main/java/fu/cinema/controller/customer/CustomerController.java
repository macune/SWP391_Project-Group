package fu.cinema.controller.customer;

import fu.cinema.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CouponService couponService;

    //Trang tổng quan
    @GetMapping("/dashboard")
    public String customerDashboard(Model model) {
        model.addAttribute("sectionTitle", "Tổng quan");
        return "customer/dashboard";
    }

    // Trang Vé của tôi
    @GetMapping("/bookings")
    public String myBookings(Model model) {
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        return "customer/dashboard";
    }

    //Trang Hồ sơ cá nhân
    @GetMapping("/profile")
    public String myProfile(Model model) {
        model.addAttribute("sectionTitle", "Hồ sơ cá nhân");
        return "customer/dashboard";
    }

    // Trang Ưu đãi & Khuyến mãi (Đã gộp logic lấy danh sách mã giảm giá)
    @GetMapping("/promotions")
    public String myPromotions(Model model) {
        model.addAttribute("sectionTitle", "Ưu đãi & Khuyến mãi của tôi");

        model.addAttribute("coupons", couponService.getActiveCoupons());

        return "customer/promotions";
    }

    // Trang Đổi mật khẩu
    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("sectionTitle", "Đổi mật khẩu bảo mật");
        return "customer/dashboard";
    }
}