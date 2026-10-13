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