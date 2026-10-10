
package fu.cinema.controller.manager;

import fu.cinema.dto.request.ShowtimeRequest;
import fu.cinema.entity.Hall;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
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
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("showtime", new ShowtimeRequest());
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute(
                "halls",
                hallRepository.findByBranchBranchId(branchId)
        );
        model.addAttribute("branchId", branchId);

        return "manager/create-showtime";
    }

    @GetMapping
    public String showSchedule(
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate showDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        Long branchId = getCurrentManagerBranchId();

        List<Hall> halls =
                hallRepository.findByBranchBranchId(branchId);

        List<Showtime> showtimes = showtimeRepository
                .findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);

        if (hallId != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getHall().getHallId().equals(hallId))
                    .toList();
        }

        if (showDate != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getStartTime().toLocalDate().equals(showDate))
                    .toList();
        }

        if (month != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getStartTime().getMonthValue() == month)
                    .toList();
        }

        if (year != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getStartTime().getYear() == year)
                    .toList();
        }

        model.addAttribute("halls", halls);
        model.addAttribute("hallId", hallId);
        model.addAttribute("showDate", showDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("showtimes", showtimes);

        return "manager/showtime-schedule";
    }

    @PostMapping("/create")
    public String createShowtime(
            @Valid @ModelAttribute("showtime") ShowtimeRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long branchId = getCurrentManagerBranchId();

        if (result.hasErrors()) {
            loadFormData(model, branchId);
            return "manager/create-showtime";
        }

        try {
            showtimeService.createShowtime(request, branchId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Showtime created successfully!"
            );

            return "redirect:/manager/showtimes";

        } catch (ShowtimeConflictException | IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            loadFormData(model, branchId);
            return "manager/create-showtime";
        }
    }

    private void loadFormData(Model model, Long branchId) {
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute(
                "earliestDateTime",
                LocalDateTime.now().plusHours(24)
        );
        model.addAttribute(
                "halls",
                hallRepository.findByBranchBranchId(branchId)
        );
        model.addAttribute("branchId", branchId);
    }

    private Long getCurrentManagerBranchId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Staff staff = staffRepository.findByAccount_Username(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy thông tin Manager"));

        if (staff.getBranch() == null) {
            throw new IllegalStateException(
                    "Manager chưa được gán chi nhánh");
        }

        return staff.getBranch().getBranchId();
    }
    @PostMapping("/{id}/cancel")
    public String cancelShowtime(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            Long branchId = getCurrentManagerBranchId();
            showtimeService.cancelShowtime(id, branchId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Showtime cancelled successfully!"
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/manager/showtimes";
    }

}
