package fu.cinema.controller.customer;

import fu.cinema.entity.Booking;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.BookingFnBService;
import fu.cinema.service.BranchStockService;
import fu.cinema.service.FnBItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer/booking")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final FnBItemService fnBItemService;
    private final BranchStockService branchStockService;
    private final BookingRepository bookingRepository;
    private final BookingFnBService bookingFnBService;

    // 1. HIỂN THỊ GIAO DIỆN CHỌN BẮP NƯỚC CHO ĐƠN HÀNG
    @GetMapping("/{bookingId}/fnb")
    public String showFnBSelectionPage(@PathVariable Long bookingId, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Đã bọc try-catch: Nếu không tìm thấy đơn, sẽ không bị nổ lỗi thô ra màn hình
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng để thêm bắp nước!"));

            if (booking.getTickets() == null || booking.getTickets().isEmpty()) {
                throw new RuntimeException("Đơn hàng chưa có vé, không thể xác định chi nhánh để mua bắp nước!");
            }

            Long branchId = booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId();

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

            return "customer/select-fnb";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/"; // Hoặc redirect về trang chủ/trang chọn phim
        }
    }

    // 2. THÊM MÓN VÀO GIỎ HÀNG
    @PostMapping("/add")
    public String addFnB(@RequestParam Long bookingId, @RequestParam Long itemId, @RequestParam int quantity) {
        bookingFnBService.addFnBToBooking(bookingId, itemId, quantity);
        return "redirect:/customer/booking/" + bookingId + "/fnb";
    }

    // 3. XÓA MÓN KHỎI GIỎ HÀNG
    @PostMapping("/remove")
    public String removeFnB(@RequestParam Long bookingId, @RequestParam Long bookingFnbId) {
        bookingFnBService.removeFnBFromBooking(bookingFnbId);
        return "redirect:/customer/booking/" + bookingId + "/fnb";
    }

    // 4. CẬP NHẬT SỐ LƯỢNG MÓN
    @PostMapping("/update")
    public String updateFnBQuantity(@RequestParam Long bookingId, @RequestParam Long bookingFnbId, @RequestParam int quantity) {
        if (quantity <= 0) bookingFnBService.removeFnBFromBooking(bookingFnbId);
        else bookingFnBService.updateFnBQuantity(bookingFnbId, quantity);

        return "redirect:/customer/booking/" + bookingId + "/fnb";
    }
}