package fu.cinema.controller.staff;

import fu.cinema.dto.response.TicketValidationResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.exception.AppException;
import fu.cinema.service.AccountService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffTicketController {

    private final AccountService accountService;
    private final TicketService ticketService;

    @GetMapping("/check-in")
    public String checkInPage(Authentication authentication, Model model) {
        if (authentication != null) {
            try {
                Account acc = accountService.getAccountByUsername(authentication.getName());
                if (acc.getStaff() != null && acc.getStaff().getBranch() != null) {
                    Branch currentBranch = acc.getStaff().getBranch();
                    model.addAttribute("currentBranch", currentBranch);
                }
            } catch (Exception ignored) {}
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
}
