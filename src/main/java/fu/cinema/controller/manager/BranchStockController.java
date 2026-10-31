package fu.cinema.controller.manager;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.FnBItem;
import fu.cinema.entity.Staff;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager/inventory")
@RequiredArgsConstructor
public class BranchStockController {

    private final BranchStockService branchStockService;
    private final FnBItemRepository fnbItemRepository;
    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;

    // Lấy chính xác ID chi nhánh của Quản lý đang đăng nhập từ Security Context
    private Long getCurrentManagerBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();

            Account acc = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            Staff manager = staffRepository.findById(acc.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

            return manager.getBranch().getBranchId();
        }

        throw new RuntimeException("Yêu cầu đăng nhập");
    }

    // 1. Xem danh sách Tồn kho (Chỉ lấy dữ liệu kho của đúng rạp mà Manager đó quản lý)
    @GetMapping
    public String viewInventory(Model model) {
        Long managerBranchId = getCurrentManagerBranchId();
        model.addAttribute("stocks", branchStockService.getStockByBranch(managerBranchId));

        // Chỉ load lên form nhập các món bắp nước đang được Admin cho phép bán
        List<FnBItem> fnbItems = fnbItemRepository.findAll().stream()
                .filter(FnBItem::getIsAvailable)
                .toList();
        model.addAttribute("fnbItems", fnbItems);

        // Chuẩn bị form nhập kho rỗng, ghim sẵn ID chi nhánh để chống nhập nhầm rạp
        if (!model.containsAttribute("stockRequest")) {
            BranchStockRequest request = new BranchStockRequest();
            request.setBranchId(managerBranchId);
            model.addAttribute("stockRequest", request);
        }

        return "manager/inventory-management";
    }

    // 2. Nhập thêm hàng mới vào kho rạp
    @PostMapping("/import")
    public String importStock(@ModelAttribute("stockRequest") BranchStockRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Ghi đè lại ID rạp một lần nữa ở Backend để bảo mật, đề phòng HTML bị hack đổi ID
            request.setBranchId(getCurrentManagerBranchId());

            branchStockService.importStock(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã nhập kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/inventory";
    }

    // 3. Cập nhật/Kiểm kê lại số lượng hàng (Ghi đè số lượng mới)
    @PostMapping("/update/{id}")
    public String updateStock(@PathVariable Long id, @RequestParam Integer quantity, RedirectAttributes redirectAttributes) {
        try {
            branchStockService.updateStock(id, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật số lượng kiểm kê thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/inventory";
    }

    // 4. Xóa mặt hàng khỏi kho của rạp
    @PostMapping("/delete/{id}")
    public String deleteStock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            branchStockService.deleteStock(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bản ghi tồn kho khỏi chi nhánh!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/manager/inventory";
    }
}