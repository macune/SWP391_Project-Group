package fu.cinema.controller.manager;

import fu.cinema.dto.response.ShowtimeAnalyticsResponse;
import fu.cinema.entity.Staff;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.service.RevenueReportService;
import fu.cinema.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Arrays;

@Controller
@RequestMapping("/manager/showtime-analytics")
@RequiredArgsConstructor
public class ManagerShowtimeAnalyticsController {

    private final RevenueReportService revenueReportService;
    private final StaffService staffService;

    @GetMapping
    public String showAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "movie") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff manager = staffService.getManagerByUsername(username);
        Long branchId = manager.getBranch().getBranchId();

        ShowtimeAnalyticsResponse analytics = revenueReportService.getShowtimeAnalytics(
                branchId, startDate, endDate, movieId, status, sortBy, sortDir, page, 10
        );

        if (analytics.dateError() != null) {
            model.addAttribute("dateError", analytics.dateError());
        }

        model.addAttribute("currentBranch", manager.getBranch());
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

        return "manager/showtime-analytics";
    }
}
