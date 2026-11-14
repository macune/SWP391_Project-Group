package fu.cinema.controller.admin;

import fu.cinema.dto.response.BranchResponse;
import fu.cinema.dto.response.ShowtimeAnalyticsResponse;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.service.BranchService;
import fu.cinema.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/showtime-analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShowtimeAnalyticsController {

    private final RevenueReportService revenueReportService;
    private final BranchService branchService;

    @GetMapping
    public String showAnalytics(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "movie") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        List<BranchResponse> branches = branchService.getAllBranches();
        if (branchId != null && branches.stream().noneMatch(b -> b.getBranchId().equals(branchId))) {
            throw new IllegalArgumentException("Chi nhánh không tồn tại.");
        }

        ShowtimeAnalyticsResponse analytics = revenueReportService.getShowtimeAnalytics(
                branchId, startDate, endDate, movieId, status, sortBy, sortDir, page, 10
        );

        if (analytics.dateError() != null) {
            model.addAttribute("dateError", analytics.dateError());
        }

        model.addAttribute("branches", branches);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("branchLabel", branchId == null
                ? "Toàn bộ chi nhánh"
                : branches.stream()
                .filter(b -> b.getBranchId().equals(branchId))
                .map(BranchResponse::getBranchName)
                .findFirst().orElse("Chi nhánh"));

        model.addAttribute("startDate", analytics.startDate());
        model.addAttribute("endDate", analytics.endDate());
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", Arrays.asList(ShowtimeStatus.values()));
        model.addAttribute("movies", analytics.movies());

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        model.addAttribute("totalShowtimes", analytics.totalShowtimes());
        model.addAttribute("totalTickets", analytics.totalTickets());
        model.addAttribute("totalSeats", analytics.totalSeats());
        model.addAttribute("occupancyRate", analytics.occupancyRate());
        model.addAttribute("ticketRevenue", analytics.ticketRevenue());

        model.addAttribute("hourLabels", analytics.hourLabels());
        model.addAttribute("hourValues", analytics.hourValues());
        model.addAttribute("dayLabels", analytics.dayLabels());
        model.addAttribute("dayValues", analytics.dayValues());

        model.addAttribute("showtimeRows", analytics.showtimeRows());
        model.addAttribute("currentPage", analytics.currentPage());
        model.addAttribute("totalPages", analytics.totalPages());
        model.addAttribute("totalElements", analytics.totalElements());
        model.addAttribute("pageSize", analytics.pageSize());

        return "admin/showtime-analytics";
    }
}
