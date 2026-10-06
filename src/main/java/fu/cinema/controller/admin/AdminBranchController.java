package fu.cinema.controller.admin;

import fu.cinema.dto.request.BranchRequest;
import fu.cinema.enums.BranchStatus;
import fu.cinema.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/branches")
@RequiredArgsConstructor
public class AdminBranchController {

    private final BranchService branchService;

    @GetMapping
    public String listBranches(Model model) {
        model.addAttribute("branches", branchService.getAllBranches());
        if (!model.containsAttribute("newBranch")) {
            model.addAttribute("newBranch", new BranchRequest());
        }
        model.addAttribute("statuses", BranchStatus.values());
        model.addAttribute("eligibleUsers", branchService.getEligibleUsersForManager());
        return "admin/branch-list";
    }

    @PostMapping("/add")
    public String addBranch(@Valid @ModelAttribute("newBranch") BranchRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/admin/branches";
        }
        try {
            branchService.createBranch(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm chi nhánh mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/edit")
    public String editBranch(@Valid @ModelAttribute("editBranch") BranchRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/admin/branches";
        }
        try {
            branchService.updateBranch(request);
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

    @PostMapping("/{branchId}/managers/assign")
    public String assignManager(@PathVariable Long branchId,
                                @RequestParam("accountId") Long accountId,
                                RedirectAttributes redirectAttributes) {
        try {
            branchService.assignManager(branchId, accountId);
            redirectAttributes.addFlashAttribute("successMessage", "Phân công Quản lý cho chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/{branchId}/managers/remove")
    public String removeManager(@PathVariable Long branchId, RedirectAttributes redirectAttributes) {
        try {
            branchService.removeManager(branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ Quản lý khỏi chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/branches";
    }
}