package fu.cinema.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    // 1. Trang mặc định khi Manager đăng nhập: Xếp lịch chiếu rạp
    @GetMapping({"", "/", "/showtimes"})
    public String showtimeSchedulePage() {
        return "manager/showtime-schedule";
    }

    // 2. Trang Quản lý Bắp nước (F&B)
    @GetMapping("/fnb-items")
    public String fnbItemsPage() {
        return "manager/fnb-items";
    }
}