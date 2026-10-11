package fu.cinema.controller.manager;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.entity.FnBItem;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public String viewInventory(Model model) {
        Long managerBranchId = 1L;

        model.addAttribute("stocks", branchStockService.getStockByBranch(managerBranchId));

        List<FnBItem> fnbItems = fnbItemRepository.findAll();
        model.addAttribute("fnbItems", fnbItems);

        if (!model.containsAttribute("stockRequest")) {
            BranchStockRequest request = new BranchStockRequest();
            request.setBranchId(managerBranchId);
            model.addAttribute("stockRequest", request);
        }

        return "manager/inventory-management";
    }

    @PostMapping("/import")
    public String importStock(@ModelAttribute("stockRequest") BranchStockRequest request, RedirectAttributes redirectAttributes) {
        try {
            branchStockService.importStock(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã nhập kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/inventory";
    }

    // --- THÊM API SỬA (GHI ĐÈ SỐ LƯỢNG) ---
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

    // --- THÊM API XÓA ---
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