package fu.cinema.controller.manager;

import fu.cinema.dto.request.ShowtimeRequest;
import fu.cinema.entity.Hall;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.exception.ShowtimeConflictException;
import fu.cinema.repository.HallRepository;
import fu.cinema.repository.MovieRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.ShowtimeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/manager/showtimes")
@RequiredArgsConstructor
public class ManagerShowtimeController {

    private final ShowtimeService showtimeService;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final StaffRepository staffRepository;
    private final ShowtimeRepository showtimeRepository;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        Long branchId = getCurrentManagerBranchId();

        // Chỉ tạo mới object nếu chưa có (để giữ lại dữ liệu khi redirect vì lỗi)
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

        // NẾU CÓ LỖI VALIDATION -> Gửi dữ liệu qua Redirect để giữ lại trên form
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
            // BẮT LỖI XUNG ĐỘT (Exception) VÀ ĐẨY RA GIAO DIỆN
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("showtime", request);
            return "redirect:/manager/showtimes/create";
        }
    }

    @GetMapping
    public String showSchedule(
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate showDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        Long branchId = getCurrentManagerBranchId();
        List<Hall> halls = hallRepository.findByBranchBranchId(branchId);
        List<Showtime> showtimes = showtimeRepository.findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);

        if (status != null) showtimes = showtimes.stream().filter(s -> s.getStatus() == status).toList();
        if (hallId != null) showtimes = showtimes.stream().filter(s -> s.getHall().getHallId().equals(hallId)).toList();
        if (showDate != null) showtimes = showtimes.stream().filter(s -> s.getStartTime().toLocalDate().equals(showDate)).toList();
        if (month != null) showtimes = showtimes.stream().filter(s -> s.getStartTime().getMonthValue() == month).toList();
        if (year != null) showtimes = showtimes.stream().filter(s -> s.getStartTime().getYear() == year).toList();

        model.addAttribute("halls", halls);
        model.addAttribute("hallId", hallId);
        model.addAttribute("showDate", showDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ShowtimeStatus.values());
        return "manager/showtime-schedule";
    }

    private void loadFormData(Model model, Long branchId) {
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("movies", movieRepository.findAll().stream().filter(movie -> movie.getStatus() != MovieStatus.ENDED).toList());
        model.addAttribute("earliestDateTime", LocalDateTime.now().plusHours(24));
        model.addAttribute("halls", hallRepository.findByBranchBranchId(branchId));
        model.addAttribute("branchId", branchId);
    }

    private Long getCurrentManagerBranchId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff staff = staffRepository.findByAccount_Username(username).orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin Manager"));
        if (staff.getBranch() == null) throw new IllegalStateException("Manager chưa được gán chi nhánh");
        return staff.getBranch().getBranchId();
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
            Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
            if (showtime.getHall().getBranch() == null || !showtime.getHall().getBranch().getBranchId().equals(branchId)) {
                throw new IllegalArgumentException("Bạn không có quyền sửa suất chiếu này.");
            }
            if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
                throw new IllegalArgumentException("Chỉ có thể sửa suất chiếu đã lên lịch.");
            }

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
}