package fu.cinema.controller.staff;

import fu.cinema.dto.response.TicketValidationResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.exception.AppException;
import fu.cinema.repository.AccountRepository;
import fu.cinema.service.BranchStockService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final BranchStockService branchStockService;
    private final AccountRepository accountRepository;
    private final TicketService ticketService;

    @GetMapping("/fnb")
    public String fnbBookingPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account staffAcc = accountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Long branchId = staffAcc.getStaff().getBranch().getBranchId();
        model.addAttribute("menuItems", branchStockService.getStockByBranch(branchId));

        return "staff/fnb-booking";
    }

    // Màn hình Soát vé vào phòng bằng Camera QR & In vé vật lý
    @GetMapping("/check-in")
    public String checkInPage(Authentication authentication, Model model) {
        if (authentication != null) {
            accountRepository.findByUsername(authentication.getName()).ifPresent(acc -> {
                if (acc.getStaff() != null && acc.getStaff().getBranch() != null) {
                    Branch currentBranch = acc.getStaff().getBranch();
                    model.addAttribute("currentBranch", currentBranch);
                }
            });
        }
        return "staff/ticket-checkin";
    }

    // API tra cứu & kiểm tra vé ngay khi Camera quét trúng mã QR
    @GetMapping("/api/tickets/validate")
    @ResponseBody
    public ResponseEntity<TicketValidationResponse> validateQrTicket(
            @RequestParam("qrCode") String qrCode,
            Authentication authentication) {
        String staffUsername = authentication != null ? authentication.getName() : null;
        TicketValidationResponse result = ticketService.validateTicketByQr(qrCode, staffUsername);
        return ResponseEntity.ok(result);
    }

    // API xác nhận Check-in (Cho khách vào phòng chiếu)
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