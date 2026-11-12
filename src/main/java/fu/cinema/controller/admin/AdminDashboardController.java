package fu.cinema.controller.admin;

import fu.cinema.dto.response.DashboardResponse;
import fu.cinema.service.BranchService;
import fu.cinema.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final BranchService branchService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "week") String period,
            Model model) {

        DashboardResponse dashboard = dashboardService.getDashboardData(branchId, period);

        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("period", dashboard.period());
        model.addAttribute("periodLabel", dashboard.periodLabel());
        model.addAttribute("totalRevenue", dashboard.totalRevenue());
        model.addAttribute("totalTickets", dashboard.totalTickets());
        model.addAttribute("totalShowtimes", dashboard.totalShowtimes());
        model.addAttribute("occupancyRate", dashboard.occupancyRate());
        model.addAttribute("chartLabels", dashboard.chartLabels());
        model.addAttribute("chartValues", dashboard.chartValues());
        model.addAttribute("topMovies", dashboard.topMovies());
        model.addAttribute("upcomingShowtimes", dashboard.upcomingShowtimes());

        return "admin/dashboard";
    }
}
