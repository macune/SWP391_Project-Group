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

    // 1. Hiển thị màn hình chọn bắp nước tại quầy (POS) cho nhân viên
    @GetMapping("/booking/{bookingId}/fnb")
    public String showPosFnBPage(@PathVariable Long bookingId, Model model, Authentication auth) {
        Booking booking = bookingService.getBookingById(bookingId);
        Long branchId = (booking.getTickets() != null && !booking.getTickets().isEmpty())
                ? booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId()
                : getCurrentStaffAccount(auth).getStaff().getBranch().getBranchId();

        // Đẩy toàn bộ xử lý lọc kho và menu bắp nước xuống Service
        model.addAttribute("fnbList", branchStockService.getAvailableFnBItemsForBranch(branchId));
        model.addAttribute("stockQuantityMap", branchStockService.getStockQuantityMap(branchId));
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("cartItems", booking.getBookingFnbs());
        model.addAttribute("totalAmount", booking.getTotalAmount());

        return "staff/fnb-booking";
    }

    // 2. Thêm món F&B vào đơn hàng tại quầy
    @PostMapping("/booking/add")
    public String addFnB(@RequestParam Long bookingId, @RequestParam Long itemId, @RequestParam int quantity) {
        bookingFnBService.addFnBToBooking(bookingId, itemId, quantity);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // 3. Xóa hoàn toàn một món F&B khỏi đơn hàng tại quầy
    @PostMapping("/booking/remove")
    public String removeFnB(@RequestParam Long bookingId, @RequestParam Long bookingFnbId) {
        bookingFnBService.removeFnBFromBooking(bookingFnbId);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // 4. Cập nhật số lượng món F&B (về <= 0 thì tự xóa)
    @PostMapping("/booking/update")
    public String updateFnBQuantity(@RequestParam Long bookingId, @RequestParam Long bookingFnbId, @RequestParam int quantity) {
        if (quantity <= 0) bookingFnBService.removeFnBFromBooking(bookingFnbId);
        else bookingFnBService.updateFnBQuantity(bookingFnbId, quantity);

        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }
}