package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.repository.AccountRepository;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final BranchStockService branchStockService;
    private final AccountRepository accountRepository;

    @GetMapping("/pos")
    public String posBookingPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account staffAcc = accountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Long branchId = staffAcc.getStaff().getBranch().getBranchId();

        model.addAttribute("menuItems", branchStockService.getStockByBranch(branchId));

        return "staff/pos-booking";
    }

    @GetMapping("/check-in")
    public String checkInPage() {
        // Redirect về trang pos để đảm bảo Model được load đầy đủ dữ liệu bắp nước
        return "redirect:/staff/pos";
    }
}