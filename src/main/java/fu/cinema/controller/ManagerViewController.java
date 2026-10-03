package fu.cinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager")
public class ManagerViewController {

    @GetMapping("/fnb-items")
    public String fnbItems() {
        return "manager/fnb-items";
    }
}