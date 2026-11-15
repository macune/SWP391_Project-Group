package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final BranchStockService branchStockService;
    private final BookingFnBService bookingFnBService;
    private final BookingService bookingService;
    private final AccountService accountService;

    private Account getCurrentStaffAccount(Authentication auth) {
        return accountService.getAccountByUsername(auth.getName());
    }

    // Hiển thị màn hình chọn bắp nước tại quầy (POS) cho nhân viên
    @GetMapping("/booking/{bookingId}/fnb")
    public String showPosFnBPage(@PathVariable Long bookingId, Model model, Authentication auth) {
        Booking booking = bookingService.getBookingById(bookingId);
        Long branchId = (booking.getTickets() != null && !booking.getTickets().isEmpty())
                ? booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId()
                : getCurrentStaffAccount(auth).getStaff().getBranch().getBranchId();


        model.addAttribute("fnbList", branchStockService.getAvailableFnBItemsForBranch(branchId));
        model.addAttribute("stockQuantityMap", branchStockService.getStockQuantityMap(branchId));
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("cartItems", booking.getBookingFnbs());
        model.addAttribute("totalAmount", booking.getTotalAmount());

        return "staff/fnb-booking";
    }

    // Thêm F&B
    @PostMapping("/booking/add")
    public String addFnB(@RequestParam Long bookingId, @RequestParam Long itemId, @RequestParam int quantity) {
        bookingFnBService.addFnBToBooking(bookingId, itemId, quantity);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    //Xóa một món F&B khỏi đơn hàng tại quầy
    @PostMapping("/booking/remove")
    public String removeFnB(@RequestParam Long bookingId, @RequestParam Long bookingFnbId) {
        bookingFnBService.removeFnBFromBooking(bookingFnbId);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // Cập nhật số lượng món F&B
    @PostMapping("/booking/update")
    public String updateFnBQuantity(@RequestParam Long bookingId, @RequestParam Long bookingFnbId, @RequestParam int quantity) {
        if (quantity <= 0) bookingFnBService.removeFnBFromBooking(bookingFnbId);
        else bookingFnBService.updateFnBQuantity(bookingFnbId, quantity);

        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }
}