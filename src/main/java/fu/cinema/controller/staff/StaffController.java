package fu.cinema.controller.staff;

import fu.cinema.service.FnBItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Nhớ import Model
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {

    // 1. Tiêm Service vào Controller
    @Autowired
    private FnBItemService fnBItemService;

    // 2. Màn hình Bán vé & Bắp nước tại quầy (POS)
    @GetMapping("/pos")
    public String posBookingPage(Model model) {
        // 3. Gọi hàm lấy tất cả món F&B và đẩy vào biến "menuItems"
        model.addAttribute("menuItems", fnBItemService.GetAllItems());

        return "staff/pos-booking";
    }

    // 2. Màn hình Soát vé QR... (Giữ nguyên)
    @GetMapping("/check-in")
    public String checkInPage() {
        return "staff/pos-booking";
    }
}