package fu.cinema.controller.manager;

import fu.cinema.entity.Hall;
import fu.cinema.enums.HallStatus;
import fu.cinema.service.HallService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/halls")
public class ManagerHallController {

    private final HallService hallService;

    public ManagerHallController(HallService hallService) {
        this.hallService = hallService;
    }

    private final Long TEST_BRANCH_ID = 1L;

    @GetMapping
    public String listHalls(Model model) {
        model.addAttribute("halls", hallService.getHallsByBranch(TEST_BRANCH_ID));
        if (!model.containsAttribute("newHall")) {
            model.addAttribute("newHall", new Hall());
        }
        model.addAttribute("statuses", HallStatus.values());
        model.addAttribute("branchId", TEST_BRANCH_ID);
        return "manager/hall-list";
    }

    @PostMapping("/add")
    public String addHall(@ModelAttribute("newHall") Hall hall, RedirectAttributes redirectAttributes) {
        try {
            hallService.createHall(hall, TEST_BRANCH_ID);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng chiếu mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/halls";
    }

    @PostMapping("/edit")
    public String editHall(@ModelAttribute("editHall") Hall hall, RedirectAttributes redirectAttributes) {
        try {
            hallService.updateHall(hall, TEST_BRANCH_ID);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng chiếu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/halls";
    }

    @PostMapping("/delete/{id}")
    public String deleteHall(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hallService.deleteHall(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng chiếu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/halls";
    }
}