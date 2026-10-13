package fu.cinema.controller.admin;

import fu.cinema.dto.request.ManagerRequest;
import fu.cinema.dto.response.AccountResponse;
import fu.cinema.enums.AccountStatus;
import fu.cinema.exception.AppException;
import fu.cinema.service.*;
import java.util.List;
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
    AccountService accountService;

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

    // Trang quản lý danh sách các tài khoản & tìm kiếm theo username
    @GetMapping("/accounts")
    public String listAccounts(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        List<AccountResponse> accounts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            accounts = accountService.searchAccounts(keyword.trim());
        } else {
            accounts = accountService.getAllAccounts();
        }
        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);
        return "admin/account-list";
    }

    // Đổi trạng thái tài khoản
    @PostMapping("/accounts/update-status/{id}")
    public String updateAccountStatus(@PathVariable Long id,
                                       @RequestParam("status") AccountStatus status,
                                       RedirectAttributes redirectAttributes) {
        try {
            accountService.changeAccountStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái tài khoản thành công.");
            return "redirect:/admin/accounts";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/accounts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            return "redirect:/admin/accounts";
        }
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