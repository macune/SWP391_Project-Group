package fu.cinema.controller.staff;

import fu.cinema.dto.response.DashboardResponse;
import fu.cinema.service.BookingService;
import fu.cinema.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final DashboardService dashboardService;
    private final BookingService bookingService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication != null
                ? authentication.getName()
                : SecurityContextHolder.getContext().getAuthentication().getName();

        DashboardResponse.Staff data = dashboardService.getStaffDashboardData(username);

        model.addAttribute("branchName", data.branchName());
        model.addAttribute("today", data.today());
        model.addAttribute("dailyRevenue", data.dailyRevenue());
        model.addAttribute("ticketRevenue", data.ticketRevenue());
        model.addAttribute("fnbRevenue", data.fnbRevenue());
        model.addAttribute("todayShowtimesCount", data.todayShowtimesCount());
        model.addAttribute("availableFnbItemsCount", data.availableFnbItemsCount());
        model.addAttribute("upcomingShowtimes", data.upcomingShowtimes());

        return "staff/dashboard";
    }

    @GetMapping("/fnb")
    public String fnbBookingPage(Authentication auth) {
        Long bookingId = bookingService.getOrCreatePendingRetailBooking(auth.getName());
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }
}