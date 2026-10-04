package fu.cinema.controller;

import fu.cinema.entity.Branch;
import fu.cinema.enums.BranchStatus;
import fu.cinema.service.BranchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/branches")
public class AdminBranchController {

    private final BranchService branchService;

    public AdminBranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public String listBranches(Model model) {
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("newBranch", new Branch());
        model.addAttribute("statuses", BranchStatus.values());
        return "admin/branch-list";
    }

    @PostMapping("/add")
    public String addBranch(@ModelAttribute("newBranch") Branch branch, Model model) {
        try {
            branchService.createBranch(branch);
            model.addAttribute("successMessage", "Thêm chi nhánh mới thành công!");
            model.addAttribute("newBranch", new Branch()); // Reset form
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        // Nạp lại danh sách chi nhánh & enum status để hiển thị lại trang
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("statuses", BranchStatus.values());
        return "admin/branch-list";
    }
}