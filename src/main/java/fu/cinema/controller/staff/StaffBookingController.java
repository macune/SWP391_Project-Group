package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.BookingFnBService;
import fu.cinema.service.BranchStockService;
import fu.cinema.service.FnBItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff/booking")
@RequiredArgsConstructor
public class StaffBookingController {

    private final FnBItemService fnBItemService;
    private final BranchStockService branchStockService;
    private final BookingRepository bookingRepository;
    private final BookingFnBService bookingFnBService;
    private final AccountRepository accountRepository;

    // 1. HIỂN THỊ MÀN HÌNH CHỌN MÓN TẠI QUẦY (POS)
    @GetMapping("/{bookingId}/fnb")
    public String showPosFnBPage(@PathVariable Long bookingId, Model model, Authentication auth) {
        // Lấy đơn hàng hiện tại
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Long branchId;

        // Xác định rạp: Nếu đơn đã có vé thì lấy rạp theo vé.
        // Nếu khách mua bắp nước vãng lai (chưa có vé), lấy rạp theo nhân viên đang trực quầy.
        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            branchId = booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId();
        } else {
            Account staffAcc = accountRepository.findByUsername(auth.getName()).orElseThrow();
            branchId = staffAcc.getStaff().getBranch().getBranchId();
        }

        // Khởi tạo sổ ghi chép kho
        List<Long> availableItemIds = new ArrayList<>();
        Map<Long, Integer> stockQuantityMap = new HashMap<>();

        // Lấy số lượng hàng tồn kho của đúng rạp hiện tại
        branchStockService.getStockByBranch(branchId).forEach(stock -> {
            availableItemIds.add(stock.getItemId());
            stockQuantityMap.put(stock.getItemId(), stock.getQuantity() != null ? stock.getQuantity() : 0);
        });

        // Lọc Menu: Chỉ hiện các món Admin cho phép bán VÀ rạp đang có sẵn hàng
        List<FnBItemResponse> fnbList = fnBItemService.GetAllItems().stream()
                .filter(FnBItemResponse::getIsAvailable)
                .filter(item -> availableItemIds.contains(item.getId()))
                .toList();

        // Đẩy toàn bộ dữ liệu (Menu, Tồn kho, Giỏ hàng, Tổng tiền) ra giao diện POS
        model.addAttribute("fnbList", fnbList);
        model.addAttribute("stockQuantityMap", stockQuantityMap);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("cartItems", booking.getBookingFnbs());
        model.addAttribute("totalAmount", booking.getTotalAmount());

        return "staff/fnb-booking";
    }

    // 2. THÊM MÓN VÀO ĐƠN
    @PostMapping("/add")
    public String addFnB(@RequestParam Long bookingId, @RequestParam Long itemId, @RequestParam int quantity) {
        bookingFnBService.addFnBToBooking(bookingId, itemId, quantity);
        return "redirect:/staff/booking/" + bookingId + "/fnb"; // Trả về lại màn hình POS
    }

    // 3. XÓA MÓN KHỎI ĐƠN
    @PostMapping("/remove")
    public String removeFnB(@RequestParam Long bookingId, @RequestParam Long bookingFnbId) {
        bookingFnBService.removeFnBFromBooking(bookingFnbId);
        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }

    // 4. CẬP NHẬT SỐ LƯỢNG MÓN
    @PostMapping("/update")
    public String updateFnBQuantity(@RequestParam Long bookingId, @RequestParam Long bookingFnbId, @RequestParam int quantity) {
        // Tự động xóa nếu nhân viên bấm giảm số lượng về <= 0
        if (quantity <= 0) bookingFnBService.removeFnBFromBooking(bookingFnbId);
        else bookingFnBService.updateFnBQuantity(bookingFnbId, quantity);

        return "redirect:/staff/booking/" + bookingId + "/fnb";
    }
}