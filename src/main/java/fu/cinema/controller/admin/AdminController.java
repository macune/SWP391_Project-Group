package fu.cinema.controller.admin;

import fu.cinema.dto.request.ManagerRequest;
import fu.cinema.exception.AppException;
import fu.cinema.service.AdminService;
import fu.cinema.service.BranchService;
import fu.cinema.service.StaffService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    StaffService staffService;
    AdminService adminService;
    BranchService branchService;

    // Cổng điều hướng chính của Admin: Doanh thu toàn chuỗi
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    // Trang quản lý danh sách Quản lý chi nhánh (Managers)
    @GetMapping("/managers")
    public String listManagers(Model model) {
        model.addAttribute("managers", adminService.getAllManagers());
        model.addAttribute("branches", branchService.getAllBranches());
        if (!model.containsAttribute("newManager")) {
            model.addAttribute("newManager", new ManagerRequest());
        }
        return "admin/manager-list";
    }

    // Xử lý tạo mới Quản lý chi nhánh
    @PostMapping("/managers/create")
    public String createManager(@Valid @ModelAttribute("newManager") ManagerRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            redirectAttributes.addFlashAttribute("newManager", request);
            return "redirect:/admin/managers";
        }

        try {
            staffService.createManager(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản Quản lý chi nhánh mới thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("newManager", request);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("newManager", request);
        }

        return "redirect:/admin/managers";
    }

    // Xử lý cập nhật Quản lý chi nhánh
    @PostMapping("/managers/update/{id}")
    public String updateManager(@PathVariable Long id,
                                @Valid @ModelAttribute("editManager") ManagerRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/admin/managers";
        }

        try {
            staffService.updateManager(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin Quản lý thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
        }

        return "redirect:/admin/managers";
    }

    // Xử lý xóa Quản lý chi nhánh
    @PostMapping("/managers/delete/{id}")
    public String deleteManager(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            staffService.deleteManager(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản Quản lý thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
        }

        return "redirect:/admin/managers";
    }
}