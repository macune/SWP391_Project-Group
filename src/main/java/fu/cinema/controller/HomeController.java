package fu.cinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 1. Giao diện Khách hàng
    @GetMapping("/")
    public String customerHome() {
        return "customer/home";
    }

    // 2. Giao diện Đăng nhập chung
    @GetMapping("/login")
    public String loginPage() {
        return "common/login";
    }

    // 3. Giao diện System Admin
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    // 4. Giao diện Branch Manager
    @GetMapping("/manager/showtimes")
    public String managerShowtimes() {
        return "manager/showtime-schedule";
    }

    // 5. Giao diện POS Staff
    @GetMapping("/staff/pos")
    public String staffPos() {
        return "staff/pos-booking";
    }
}