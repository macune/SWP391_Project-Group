package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.dto.response.FnBItemResponse;
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
        Long branchId;

        // Nếu đơn có vé -> lấy chi nhánh theo vé, nếu là đơn bán lẻ -> lấy chi nhánh theo nhân viên đang trực quầy
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

        // Lọc danh sách món F&B đang bán và còn hàng trong kho của chi nhánh
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