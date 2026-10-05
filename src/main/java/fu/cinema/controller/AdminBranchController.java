package fu.cinema.controller;

import fu.cinema.entity.Branch;
import fu.cinema.enums.BranchStatus;
import fu.cinema.service.BranchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        if (!model.containsAttribute("newBranch")) {
            model.addAttribute("newBranch", new Branch());
        }
        model.addAttribute("statuses", BranchStatus.values());
        model.addAttribute("eligibleUsers", branchService.getEligibleUsersForManager());
        return "admin/branch-list";
    }

    @PostMapping("/add")
    public String addBranch(@ModelAttribute("newBranch") Branch branch, RedirectAttributes redirectAttributes) {
        try {
            branchService.createBranch(branch);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm chi nhánh mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/edit")
    public String editBranch(@ModelAttribute("editBranch") Branch branch, RedirectAttributes redirectAttributes) {
        try {
            branchService.updateBranch(branch);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/delete/{id}")
    public String deleteBranch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            branchService.deleteBranch(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/{branchId}/managers/add")
    public String addManager(@PathVariable Long branchId,
                             @RequestParam("accountId") Long accountId,
                             RedirectAttributes redirectAttributes) {
        try {
            branchService.assignManager(branchId, accountId);
            redirectAttributes.addFlashAttribute("successMessage", "Gán quyền Manager cho chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/{branchId}/managers/remove/{staffId}")
    public String removeManager(@PathVariable Long branchId,
                                @PathVariable Long staffId,
                                RedirectAttributes redirectAttributes) {
        try {
            branchService.removeManager(branchId, staffId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa Manager khỏi chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }
}