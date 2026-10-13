package fu.cinema.controller.manager;

import fu.cinema.dto.request.BranchRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Staff;
import fu.cinema.repository.AccountRepository;
import fu.cinema.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/branch-settings")
@RequiredArgsConstructor
public class ManagerBranchController {

    private final BranchService branchService;
    private final AccountRepository accountRepository;

    /**
     * Hàm dùng chung để lấy ID chi nhánh của Manager đang đăng nhập hiện tại
     */
    private Branch getCurrentManagerBranch(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Bạn chưa đăng nhập!");
        }

        String username = authentication.getName();
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + username));

        Staff staff = account.getStaff();
        if (staff == null || staff.getBranch() == null) {
            throw new IllegalArgumentException("Tài khoản của bạn chưa được phân công quản lý chi nhánh nào!");
        }

        return staff.getBranch();
    }

    @GetMapping
    public String showBranchSettings(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Branch currentBranch = getCurrentManagerBranch(authentication);

            if (!model.containsAttribute("branchRequest")) {
                BranchRequest request = BranchRequest.builder()
                        .branchId(currentBranch.getBranchId())
                        .branchName(currentBranch.getBranchName())
                        .address(currentBranch.getAddress())
                        .hotline(currentBranch.getHotline())
                        .openingTime(currentBranch.getOpeningTime())
                        .closingTime(currentBranch.getClosingTime())
                        .status(currentBranch.getStatus())
                        .build();
                model.addAttribute("branchRequest", request);
            }
            return "manager/branch-edit";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/edit")
    public String updateBranch(@Valid @ModelAttribute("branchRequest") BranchRequest request,
                               BindingResult bindingResult,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu nhập không hợp lệ, vui lòng kiểm tra lại.");
            return "redirect:/manager/branch-settings";
        }

        try {
            Branch currentBranch = getCurrentManagerBranch(authentication);

            request.setBranchId(currentBranch.getBranchId());

            branchService.updateBranch(request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin Chi nhánh thành công!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/branch-settings";
    }
}