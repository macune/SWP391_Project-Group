package fu.cinema.controller.manager;

import fu.cinema.dto.response.DashboardResponse;
import fu.cinema.entity.Staff;
import fu.cinema.service.DashboardService;
import fu.cinema.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/manager/dashboard")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final StaffService staffService;
    private final DashboardService dashboardService;

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "week") String period, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff manager = staffService.getManagerByUsername(username);

        Long branchId = manager.getBranch().getBranchId();
        DashboardResponse dashboard = dashboardService.getDashboardData(branchId, period);

        model.addAttribute("currentBranch", manager.getBranch());
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

        return "manager/dashboard";
    }
}
