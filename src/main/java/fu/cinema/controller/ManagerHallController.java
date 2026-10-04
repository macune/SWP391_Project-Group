package fu.cinema.controller;

import fu.cinema.entity.Hall;
import fu.cinema.enums.HallStatus;
import fu.cinema.service.HallService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/halls")
public class ManagerHallController {

    private final HallService hallService;

    public ManagerHallController(HallService hallService) {
        this.hallService = hallService;
    }

    // Tạm thời cố định ID chi nhánh thử nghiệm = 1
    private final Long TEST_BRANCH_ID = 1L;

    @GetMapping
    public String listHalls(Model model) {
        model.addAttribute("halls", hallService.getHallsByBranch(TEST_BRANCH_ID));
        model.addAttribute("newHall", new Hall());
        model.addAttribute("statuses", HallStatus.values());
        model.addAttribute("branchId", TEST_BRANCH_ID);
        return "manager/hall-list";
    }

    @PostMapping("/add")
    public String addHall(@ModelAttribute("newHall") Hall hall, Model model) {
        try {
            hallService.createHall(hall, TEST_BRANCH_ID);
            model.addAttribute("successMessage", "Thêm phòng chiếu mới thành công!");
            model.addAttribute("newHall", new Hall()); // Reset form
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("halls", hallService.getHallsByBranch(TEST_BRANCH_ID));
        model.addAttribute("statuses", HallStatus.values());
        model.addAttribute("branchId", TEST_BRANCH_ID);
        return "manager/hall-list";
    }
}