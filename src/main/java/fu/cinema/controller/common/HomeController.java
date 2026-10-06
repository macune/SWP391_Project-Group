package fu.cinema.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Trang chủ dùng chung cho Guest và toàn hệ thống (không có sidebar)
    @GetMapping({"/", "/home"})
    public String publicHome() {
        return "common/home";
    }
}