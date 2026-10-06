package fu.cinema.controller.staff;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffPosController {

    // 1. Màn hình Bán vé & Bắp nước tại quầy (POS)
    @GetMapping("/pos")
    public String posBookingPage() {
        return "staff/pos-booking";
    }

    // 2. Màn hình Soát vé QR vào phòng chiếu
    @GetMapping("/check-in")
    public String checkInPage() {
        return "staff/pos-booking"; // Tạm hiển thị chung view POS để test điều hướng không bị lỗi 404
    }
}