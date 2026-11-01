package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final FnBItemService fnBItemService;
    private final BranchStockService branchStockService;
    private final BookingRepository bookingRepository;
    private final BookingFnBService bookingFnBService;
    private final AccountRepository accountRepository;
    private final BookingService bookingService;

    // Lấy thông tin Staff đang đăng nhập
    private Account getCurrentStaffAccount(Authentication auth) {
        return accountRepository.findByUsername(auth.getName()).orElseThrow();
    }

    // ==========================================
    // PHẦN 1: QUẢN LÝ BÁN BẮP NƯỚC (POS)
    // ==========================================

    // 1. HIỂN THỊ MÀN HÌNH CHỌN MÓN TẠI QUẦY (POS)
    @GetMapping("/booking/{bookingId}/fnb")
    public String showPosFnBPage(@PathVariable Long bookingId, Model model, Authentication auth) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Long branchId;

        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            branchId = booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId();
        } else {
            Account staffAcc = getCurrentStaffAccount(auth);
            branchId = staffAcc.getStaff().getBranch().getBranchId();
        }

        List<Long> availableItemIds = new ArrayList<>();
        Map<Long, Integer> stockQuantityMap = new HashMap<>();

        branchStockService.getStockByBranch(branchId).forEach(stock -> {
            availableItemIds.add(stock.getItemId());
            stockQuantityMap.put(stock.getItemId(), stock.getQuantity() != null ? stock.getQuantity() : 0);
        });

        List<FnBItemResponse> fnbList = fnBItemService.GetAllItems().stream()
                .filter(FnBItemResponse::getIsAvailable)
                .filter(item -> availableItemIds.contains(item.getId()))
                .toList();

        model.addAttribute("fnbList", fnbList);
        model.addAttribute("stockQuantityMap", stockQuantityMap);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("cartItems", booking.getBookingFnbs());
        model.addAttribute("totalAmount", booking.getTotalAmount());

        return "staff/fnb-booking";
    }

    // 2. THÊM MÓN VÀO ĐƠN
    @PostMapping("/booking/add")
    public String addFnB(@RequestParam Long bookingId, @RequestParam Long itemId, @RequestParam int quantity) {
        bookingFnBService.addFnBToBooking(bookingId, itemId, quantity);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // 3. XÓA MÓN KHỎI ĐƠN
    @PostMapping("/booking/remove")
    public String removeFnB(@RequestParam Long bookingId, @RequestParam Long bookingFnbId) {
        bookingFnBService.removeFnBFromBooking(bookingFnbId);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // 4. CẬP NHẬT SỐ LƯỢNG MÓN
    @PostMapping("/booking/update")
    public String updateFnBQuantity(@RequestParam Long bookingId, @RequestParam Long bookingFnbId, @RequestParam int quantity) {
        if (quantity <= 0) bookingFnBService.removeFnBFromBooking(bookingFnbId);
        else bookingFnBService.updateFnBQuantity(bookingFnbId, quantity);

        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // ==========================================
    // PHẦN 2: XỬ LÝ HỦY/XÓA ĐƠN ĐẶT VÉ
    // ==========================================
    @PostMapping("/bookings/{id}/hard-delete")
    public String deletePendingBooking(@PathVariable("id") Long bookingId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            Long branchId = getCurrentStaffAccount(auth).getStaff().getBranch().getBranchId();
            bookingService.hardDeletePendingBooking(bookingId, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn đơn hàng rác (chờ thanh toán)!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings";
    }

    @PostMapping("/bookings/{id}/cancel-confirmed")
    public String cancelConfirmedBooking(@PathVariable("id") Long bookingId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            Long branchId = getCurrentStaffAccount(auth).getStaff().getBranch().getBranchId();
            bookingService.cancelConfirmedBooking(bookingId, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn thành công! Đã giải phóng ghế, trừ doanh thu và hoàn bắp nước vào kho.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/bookings";
    }
}