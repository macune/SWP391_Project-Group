package fu.cinema.controller.manager;

import fu.cinema.dto.request.HallRequest;
import fu.cinema.dto.request.SeatUpdateItem;
import fu.cinema.dto.response.HallResponse;
import fu.cinema.dto.response.SeatResponse;
import fu.cinema.entity.Branch;
import fu.cinema.enums.HallStatus;
import fu.cinema.service.HallService;
import fu.cinema.service.ManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager/halls")
@RequiredArgsConstructor
public class ManagerHallController {

    private final HallService hallService;
    private final ManagerService managerService;

    @GetMapping
    public String listHalls(Model model) {
        Branch currentBranch = managerService.getCurrentManagerBranch();
        model.addAttribute("currentBranch", currentBranch);
        model.addAttribute("statuses", HallStatus.values());

        if (!model.containsAttribute("newHall")) {
            model.addAttribute("newHall", HallRequest.builder()
                    .totalRows(8)
                    .seatsPerRow(12)
                    .status(HallStatus.ACTIVE)
                    .build());
        }

        if (currentBranch == null) {
            model.addAttribute("halls", Collections.emptyList());
            return "manager/hall-list";
        }

        Long branchId = currentBranch.getBranchId();
        model.addAttribute("halls", hallService.getHallsByBranch(branchId));
        return "manager/hall-list";
    }

    @PostMapping("/add")
    public String addHall(@Valid @ModelAttribute("newHall") HallRequest request,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/manager/halls";
        }
        try {
            Branch currentBranch = managerService.getCurrentManagerBranch();
            if (currentBranch == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản quản lý của bạn chưa được phân công chi nhánh!");
                return "redirect:/manager/halls";
            }
            HallResponse created = hallService.createHallWithSeats(currentBranch.getBranchId(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tạo phòng '" + created.getHallName() + "' và tự động sinh sơ đồ "
                            + request.getTotalRows() + " hàng × " + request.getSeatsPerRow()
                            + " ghế (" + created.getCapacity() + " ghế Standard)! Hãy thiết kế loại ghế bên dưới.");
            // Chuyển thẳng sang màn hình thiết kế sơ đồ ghế của phòng vừa tạo
            return "redirect:/manager/halls/" + created.getHallId() + "/seats";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/halls";
        }
    }

    @PostMapping("/edit")
    public String editHall(@Valid @ModelAttribute("editHall") HallRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/manager/halls";
        }
        try {
            Branch currentBranch = managerService.getCurrentManagerBranch();
            if (currentBranch == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản quản lý của bạn chưa được phân công chi nhánh!");
                return "redirect:/manager/halls";
            }
            hallService.updateHall(currentBranch.getBranchId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin phòng chiếu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/halls";
    }

    @PostMapping("/delete/{id}")
    public String deleteHall(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hallService.deleteHall(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phòng chiếu và toàn bộ sơ đồ ghế đi kèm!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa phòng chiếu: " + e.getMessage());
        }
        return "redirect:/manager/halls";
    }

    // Màn hình trực quan xem & thiết kế loại ghế cho từng phòng
    @GetMapping("/{id}/seats")
    public String viewSeatMap(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            HallResponse hall = hallService.getHallById(id);
            Map<String, List<SeatResponse>> seatMap = hallService.getSeatMapByHall(id);
            model.addAttribute("hall", hall);
            model.addAttribute("seatMap", seatMap);
            return "manager/hall-seats";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/halls";
        }
    }

    // API lưu cấu hình loại ghế (STANDARD, VIP, SWEETBOX, BROKEN)
    @PostMapping("/{id}/seats/save")
    @ResponseBody
    public ResponseEntity<?> saveSeatMap(@PathVariable Long id,
                                         @RequestBody List<SeatUpdateItem> seatUpdates) {
        try {
            hallService.updateSeatMatrix(id, seatUpdates);
            return ResponseEntity.ok(Map.of("message", "Lưu sơ đồ ghế thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}