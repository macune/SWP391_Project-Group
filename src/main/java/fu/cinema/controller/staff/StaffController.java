package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.dto.response.DashboardResponse;
import fu.cinema.dto.response.TicketValidationResponse;
import fu.cinema.entity.Branch;
import fu.cinema.enums.BookingStatus;
import fu.cinema.exception.AppException;
import fu.cinema.service.BookingService;
import fu.cinema.service.DashboardService;
import fu.cinema.service.StaffService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final DashboardService dashboardService;
    private final BookingService bookingService;
    private final TicketService ticketService;
    private final StaffService staffService;

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

    @GetMapping("/bookings")
    public String showStaffBookings(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) BookingStatus status,
                                    Model model) {
        List<BookDetailsResponse> bookingList = bookingService.getAllBookings(keyword, status);
        model.addAttribute("bookings", bookingList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "staff/booking-list";
    }

    @GetMapping("/check-in")
    public String checkInPage(Authentication authentication, Model model) {
        if (authentication != null) {
            Branch currentBranch = staffService.getStaffBranch(authentication.getName());
            if (currentBranch != null) {
                model.addAttribute("currentBranch", currentBranch);
            }
        }
        return "staff/ticket-checkin";
    }

    @GetMapping("/api/tickets/validate")
    @ResponseBody
    public ResponseEntity<TicketValidationResponse> validateQrTicket(
            @RequestParam("qrCode") String qrCode,
            Authentication authentication) {
        String staffUsername = authentication != null ? authentication.getName() : null;
        TicketValidationResponse result = ticketService.validateTicketByQr(qrCode, staffUsername);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/tickets/{ticketId}/check-in")
    @ResponseBody
    public ResponseEntity<TicketValidationResponse> confirmCheckIn(
            @PathVariable("ticketId") Long ticketId,
            Authentication authentication) {
        try {
            String staffUsername = authentication != null ? authentication.getName() : null;
            TicketValidationResponse result = ticketService.confirmTicketCheckIn(ticketId, staffUsername);
            return ResponseEntity.ok(result);
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(
                    TicketValidationResponse.builder()
                            .validForCheckIn(false)
                            .validationState("ERROR")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/bookings/{id}")
    public String showBookingDetails(@PathVariable Long id, Model model) {
        try {
            BookDetailsResponse booking = bookingService.getBookingDetail(id);
            List<String> fnbDetails = bookingService.getFnbDetailsByBookingId(id);
            model.addAttribute("booking", booking);
            model.addAttribute("fnbDetails", fnbDetails);
            return "staff/booking-details";
        } catch (Exception e) {
            return "redirect:/staff/bookings";
        }
    }
}