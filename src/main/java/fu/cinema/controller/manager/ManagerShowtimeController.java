package fu.cinema.controller.manager;

import fu.cinema.dto.request.ShowtimeRequest;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.exception.ShowtimeConflictException;
import fu.cinema.service.ShowtimeService;
import fu.cinema.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/manager/showtimes")
@RequiredArgsConstructor
public class ManagerShowtimeController {

    private final ShowtimeService showtimeService;
    private final StaffService staffService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        Long branchId = getCurrentManagerBranchId();

        if (!model.containsAttribute("showtime")) {
            model.addAttribute("showtime", new ShowtimeRequest());
        }

        loadFormData(model, branchId);
        return "manager/create-showtime";
    }

    @PostMapping("/create")
    public String createShowtime(
            @Valid @ModelAttribute("showtime") ShowtimeRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        Long branchId = getCurrentManagerBranchId();

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.showtime", result);
            redirectAttributes.addFlashAttribute("showtime", request);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin đã nhập!");
            return "redirect:/manager/showtimes/create";
        }

        if (request.getShowDate() != null && request.getShowDate().isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo suất chiếu cho một ngày trong quá khứ!");
            redirectAttributes.addFlashAttribute("showtime", request);
            return "redirect:/manager/showtimes/create";
        }

        try {
            showtimeService.createShowtime(request, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo thành công suất chiếu!");
            return "redirect:/manager/showtimes";

        } catch (ShowtimeConflictException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("showtime", request);
            return "redirect:/manager/showtimes/create";
        }
    }

    @GetMapping
    public String showSchedule(
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(required = false) String showDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Long branchId = getCurrentManagerBranchId();
        Pageable pageable = PageRequest.of(page, 10);

        LocalDate parsedShowDate = null;
        if (showDate != null && !showDate.trim().isEmpty()) {
            try {
                parsedShowDate = LocalDate.parse(showDate.trim());
            } catch (Exception e) {
                try {
                    parsedShowDate = LocalDate.parse(showDate.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception ignored) {
                }
            }
        }

        Page<Showtime> showtimePage = showtimeService.getShowtimesSchedule(
                branchId, hallId, status, parsedShowDate, month, year, pageable);

        model.addAttribute("halls", showtimeService.getHallsByBranch(branchId));
        model.addAttribute("hallId", hallId);
        model.addAttribute("showDate", parsedShowDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ShowtimeStatus.values());

        model.addAttribute("showtimes", showtimePage.getContent());
        model.addAttribute("currentPage", showtimePage.getNumber());
        model.addAttribute("totalPages", showtimePage.getTotalPages());
        model.addAttribute("totalElements", showtimePage.getTotalElements());
        model.addAttribute("pageSize", showtimePage.getSize());

        return "manager/showtime-schedule";
    }

    @PostMapping("/{id}/cancel")
    public String cancelShowtime(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long branchId = getCurrentManagerBranchId();
            showtimeService.cancelShowtime(id, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy thành công suất chiếu!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/showtimes";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long branchId = getCurrentManagerBranchId();
        try {
            Showtime showtime = showtimeService.getShowtimeForEdit(id, branchId);

            if (!model.containsAttribute("showtime")) {
                ShowtimeRequest request = new ShowtimeRequest();
                request.setMovieId(showtime.getMovie().getMovieId());
                request.setHallId(showtime.getHall().getHallId());
                request.setShowDate(showtime.getStartTime().toLocalDate());
                request.setStartTime(showtime.getStartTime().toLocalTime());
                request.setBasePrice(showtime.getBasePrice());
                model.addAttribute("showtime", request);
            }

            model.addAttribute("showtimeId", id);
            loadFormData(model, branchId);
            return "manager/edit-showtime";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/manager/showtimes";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateShowtime(
            @PathVariable Long id,
            @Valid @ModelAttribute("showtime") ShowtimeRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        Long branchId = getCurrentManagerBranchId();

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.showtime", result);
            redirectAttributes.addFlashAttribute("showtime", request);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin nhập!");
            return "redirect:/manager/showtimes/" + id + "/edit";
        }

        try {
            showtimeService.updateShowtime(id, request, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật suất chiếu thành công!");
            return "redirect:/manager/showtimes";

        } catch (ShowtimeConflictException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("showtime", request);
            return "redirect:/manager/showtimes/" + id + "/edit";
        }
    }

    private void loadFormData(Model model, Long branchId) {
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("movies", showtimeService.getActiveMovies());
        model.addAttribute("earliestDateTime", LocalDateTime.now().plusHours(24));
        model.addAttribute("halls", showtimeService.getHallsByBranch(branchId));
        model.addAttribute("branchId", branchId);
    }

    private Long getCurrentManagerBranchId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff staff = staffService.getManagerByUsername(username);
        if (staff == null || staff.getBranch() == null) {
            throw new IllegalStateException("Manager chưa được gán chi nhánh");
        }
        return staff.getBranch().getBranchId();
    }
}
