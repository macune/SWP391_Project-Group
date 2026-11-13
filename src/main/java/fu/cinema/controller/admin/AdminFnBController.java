package fu.cinema.controller.admin;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.enums.FnBCategory;
import fu.cinema.service.FnBItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/fnb-items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFnBController {

    private final FnBItemService fnbItemService;

    // Hiển thị danh sách F&B và nạp form thêm mới
    @GetMapping
    public String fnbItems(@RequestParam(required = false) String name, @RequestParam(required = false) FnBCategory category, Model model) {
        model.addAttribute("items", fnbItemService.searchItems(name, category));

        if (!model.containsAttribute("item")) {
            FnBItemRequest emptyRequest = new FnBItemRequest();
            emptyRequest.setIsAvailable(true); // Vẫn đánh dấu Còn hàng sẵn
            model.addAttribute("item", emptyRequest);
        }
        return "admin/fnb-items"; // TRỎ TỚI ADMIN HTML
    }

    // Lấy dữ liệu của 1 món F&B đổ lên form
    @GetMapping("/edit/{id}")
    public String editFnbItem(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            FnBItemResponse response = fnbItemService.GetItemById(id);

            FnBItemRequest requestForm = FnBItemRequest.builder()
                    .id(id)
                    .name(response.getName())
                    .category(response.getCategory())
                    .price(response.getPrice())
                    .isAvailable(response.getIsAvailable())
                    .imageUrl(response.getImageUrl())
                    .build();

            model.addAttribute("items", fnbItemService.GetAllItems());
            model.addAttribute("item", requestForm); // Đẩy Request lên
            return "admin/fnb-items";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/fnb-items";
        }
    }

    // Lưu món F&B: Thêm mới và Cập nhật
    @PostMapping("/save")
    public String saveFnbItem(@RequestParam(required = false) Long id, @ModelAttribute FnBItemRequest request, Model model) {
        try {
            if (id != null) fnbItemService.updateItem(id, request); // Sửa
            else fnbItemService.createItem(request); // Thêm mới

            return "redirect:/admin/fnb-items";
        } catch (Exception e) {
            // Nếu có lỗi (trùng tên), giữ lại dữ liệu Admin vừa nhập trên form và báo lỗi đỏ
            FnBItemResponse currentForm = FnBItemResponse.builder()
                    .id(id).name(request.getName()).category(request.getCategory())
                    .price(request.getPrice()).imageUrl(request.getImageUrl()).isAvailable(request.getIsAvailable()).build();
            model.addAttribute("items", fnbItemService.GetAllItems());
            model.addAttribute("item", currentForm);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/fnb-items";
        }
    }

    // Xóa một món F&B
    @PostMapping("/delete/{id}")
    public String deleteFnbItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fnbItemService.deleteItem(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("globalError", e.getMessage()); // Báo lỗi nếu dính khóa ngoại
        }
        return "redirect:/admin/fnb-items";
    }
}