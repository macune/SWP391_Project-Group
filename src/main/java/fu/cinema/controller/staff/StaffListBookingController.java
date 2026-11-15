package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.enums.BookingStatus;
import fu.cinema.service.AccountService;
import fu.cinema.service.BookingService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffListBookingController {

    private final BookingService bookingService;
    private final AccountService accountService;
    private final TicketService ticketService;

    private Account getCurrentStaffAccount(Authentication auth) {
        return accountService.getAccountByUsername(auth.getName());
    }

    private Branch getCurrentStaffBranch(Authentication auth) {
        if (auth == null) return null;
        Account acc = getCurrentStaffAccount(auth);
        if (acc != null && acc.getStaff() != null) {
            return acc.getStaff().getBranch();
        }
        return null;
    }

    // 1. DANH SÁCH ĐƠN ĐẶT VÉ THUỘC CHI NHÁNH CỦA STAFF
    @GetMapping("/bookings")
    public String showStaffBookings(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) BookingStatus status,
                                    Authentication auth,
                                    Model model) {
        Branch currentBranch = getCurrentStaffBranch(auth);
        Long branchId = currentBranch != null ? currentBranch.getBranchId() : null;

        List<BookDetailsResponse> bookingList = bookingService.getAllBookings(keyword, status, branchId);
        model.addAttribute("bookings", bookingList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("currentBranch", currentBranch);
        return "staff/booking-list";
    }

    // 2. CHI TIẾT ĐƠN ĐẶT VÉ & DANH SÁCH TICKET KÈM SOÁT VÉ / IN VÉ VẬT LÝ
    @GetMapping("/bookings/{id}")
    public String showBookingDetails(@PathVariable Long id,
                                     Authentication auth,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            Branch currentBranch = getCurrentStaffBranch(auth);
            Long branchId = currentBranch != null ? currentBranch.getBranchId() : null;

            BookDetailsResponse booking = bookingService.getBookingDetail(id, branchId);
            List<String> fnbDetails = bookingService.getFnbDetailsByBookingId(id);
            List<TicketResponse> tickets = ticketService.getTicketsByBooking(id, auth.getName());

            model.addAttribute("booking", booking);
            model.addAttribute("fnbDetails", fnbDetails);
            model.addAttribute("tickets", tickets);
            model.addAttribute("currentBranch", currentBranch);
            return "staff/booking-details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/bookings";
        }
    }

    // 3. XỬ LÝ XÓA ĐƠN RÁC (CHỜ THANH TOÁN) - ĐÃ BỔ SUNG CHECK NULL AN TOÀN
    @PostMapping("/bookings/{id}/hard-delete")
    public String deletePendingBooking(@PathVariable("id") Long bookingId,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        try {
            Branch currentBranch = getCurrentStaffBranch(auth);
            if (currentBranch == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa được gán chi nhánh!");
                return "redirect:/staff/bookings";
            }
            bookingService.hardDeletePendingBooking(bookingId, currentBranch.getBranchId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn đơn hàng rác (chờ thanh toán)!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings";
    }

    // 4. XỬ LÝ HỦY ĐƠN ĐÃ XÁC NHẬN - ĐÃ BỔ SUNG CHECK NULL AN TOÀN
    @PostMapping("/bookings/{id}/cancel-confirmed")
    public String cancelConfirmedBooking(@PathVariable("id") Long bookingId,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        try {
            Branch currentBranch = getCurrentStaffBranch(auth);
            if (currentBranch == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa được gán chi nhánh!");
                return "redirect:/staff/bookings";
            }
            bookingService.cancelConfirmedBooking(bookingId, currentBranch.getBranchId());
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn thành công! Đã giải phóng ghế, trừ doanh thu và hoàn bắp nước vào kho.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings";
    }
}