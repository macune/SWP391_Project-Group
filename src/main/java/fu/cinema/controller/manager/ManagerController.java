package fu.cinema.controller.manager;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.enums.FnBCategory;
import fu.cinema.dto.request.StaffRequest;
import fu.cinema.exception.AppException;
import fu.cinema.service.FnBItemService;
import fu.cinema.service.ManagerService;
import fu.cinema.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final FnBItemService fnbItemService;
    private final StaffService staffService;
    private final ManagerService managerService;

    // 1. Trang mặc định khi Manager đăng nhập: Xếp lịch chiếu rạp
    @GetMapping({"", "/", "/showtimes"})
    public String showtimeSchedulePage() {
        return "manager/showtime-schedule";
    }

    // 2. Danh sách & Tìm kiếm Bắp nước (Khớp 100% với fnb-items.html)
    @GetMapping("/fnb-items")
    public String fnbItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) FnBCategory category,
            Model model) {

        model.addAttribute("items", fnbItemService.searchItems(name, category));
        if (!model.containsAttribute("item")) {
            FnBItemResponse emptyItem = new FnBItemResponse();
            emptyItem.setIsAvailable(true);
            model.addAttribute("item", emptyItem);
        }
        return "manager/fnb-items";
    }

    // 3. Mở Modal Chỉnh sửa sản phẩm Bắp nước
    @GetMapping("/fnb-items/edit/{id}")
    public String editFnbItem(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            FnBItemResponse item = fnbItemService.GetItemById(id);
            model.addAttribute("items", fnbItemService.GetAllItems());
            model.addAttribute("item", item);
            return "manager/fnb-items";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/fnb-items";
        }
    }

    // 4. Lưu (Thêm mới hoặc Cập nhật) sản phẩm Bắp nước
    @PostMapping("/fnb-items/save")
    public String saveFnbItem(
            @RequestParam(required = false) Long id,
            @ModelAttribute FnBItemRequest request,
            Model model) {
        try {
            if (id != null) {
                fnbItemService.updateItem(id, request);
            } else {
                fnbItemService.createItem(request);
            }
            return "redirect:/manager/fnb-items";
        } catch (Exception e) {
            // Khi trùng tên hoặc lỗi validate, giữ lại Modal và hiện thông báo lỗi
            FnBItemResponse currentForm = FnBItemResponse.builder()
                    .id(id)
                    .name(request.getName())
                    .category(request.getCategory())
                    .price(request.getPrice())
                    .imageUrl(request.getImageUrl())
                    .isAvailable(request.getIsAvailable())
                    .build();

            model.addAttribute("items", fnbItemService.GetAllItems());
            model.addAttribute("item", currentForm);
            model.addAttribute("errorMessage", e.getMessage());
            return "manager/fnb-items";
        }
    }

    // 5. Xóa sản phẩm Bắp nước
    @PostMapping("/fnb-items/delete/{id}")
    public String deleteFnbItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fnbItemService.deleteItem(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/fnb-items";
    }



    // ==========================================
    // 6. QUẢN LÝ NHÂN VIÊN CHI NHÁNH (STAFF)
    // ==========================================

    // Hiển thị danh sách nhân viên
    @GetMapping("/staffs")
    public String listStaffs(Model model) {
        model.addAttribute("staffs", managerService.getAllStaffs());
        if (!model.containsAttribute("newStaff")) {
            model.addAttribute("newStaff", new StaffRequest());
        }
        return "manager/staff-list";
    }

    // Xử lý tạo mới nhân viên
    @PostMapping("/staffs/create")
    public String createStaff(@Valid @ModelAttribute("newStaff") StaffRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            redirectAttributes.addFlashAttribute("newStaff", request);
            return "redirect:/manager/staffs";
        }

        try {
            staffService.createStaff(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản Nhân viên mới thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("newStaff", request);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            redirectAttributes.addFlashAttribute("newStaff", request);
        }

        return "redirect:/manager/staffs";
    }

    // Xử lý cập nhật thông tin nhân viên
    @PostMapping("/staffs/update/{id}")
    public String updateStaff(@PathVariable Long id,
                              @Valid @ModelAttribute("editStaff") StaffRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/manager/staffs";
        }

        try {
            staffService.updateStaff(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin Nhân viên thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
        }

        return "redirect:/manager/staffs";
    }

    // Xử lý xóa nhân viên
    @PostMapping("/staffs/delete/{id}")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            staffService.deleteStaff(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản Nhân viên thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
        }

        return "redirect:/manager/staffs";
    }
}