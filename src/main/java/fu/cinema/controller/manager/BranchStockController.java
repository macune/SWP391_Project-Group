package fu.cinema.controller.manager;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/inventory")
@RequiredArgsConstructor
public class BranchStockController {


    private final BranchStockService branchStockService;

    //Trích xuất ID chi nhánh của Manager đang đăng nhập từ Security Context
    private Long getManagerBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return branchStockService.getCurrentManagerBranchId(auth.getName());
        }

        throw new RuntimeException("Yêu cầu đăng nhập");
    }

    // 1. Hiển thị màn hình Quản lý kho
    @GetMapping
    public String viewInventory(Model model) {
        Long managerBranchId = getManagerBranchId();

        // bảng tồn kho hiện tại của rạp
        model.addAttribute("stocks", branchStockService.getStockByBranch(managerBranchId));

        //danh sách món F&B đang mở bán
        model.addAttribute("fnbItems", branchStockService.getAvailableFnBItems());

        // Khởi tạo form rỗng
        if (!model.containsAttribute("stockRequest")) {
            BranchStockRequest request = new BranchStockRequest();
            request.setBranchId(managerBranchId);
            model.addAttribute("stockRequest", request);
        }

        return "manager/inventory-management";
    }

    // Nhập thêm hàng mới vào kho của rạp
    @PostMapping("/import")
    public String importStock(@ModelAttribute("stockRequest") BranchStockRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Bảo mật: Ép cứng ID rạp bằng thông tin Manager đang đăng nhập
            request.setBranchId(getManagerBranchId());

            branchStockService.importStock(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã nhập kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/inventory";
    }

    //Cập nhật/Kiểm kê lại số lượng hàng thực tế
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

    //Xóa bản ghi mặt hàng khỏi kho của rạp
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