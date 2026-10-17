package fu.cinema.controller.customer;

import fu.cinema.entity.Booking;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.BookingFnBService;
import fu.cinema.service.FnBItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customer/booking")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final FnBItemService fnBItemService;
    private final BookingRepository bookingRepository;
    private final BookingFnBService bookingFnBService;


    // HIỂN THỊ TRANG CHỌN BẮP NƯỚC

    @GetMapping("/{bookingId}/fnb")
    public String showFnBSelectionPage(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        List<FnBItemResponse> availableItems = fnBItemService.GetAllItems().stream()
                .filter(FnBItemResponse::getIsAvailable)
                .toList();

        model.addAttribute("fnbList", availableItems);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("cartItems", booking.getBookingFnbs());
        model.addAttribute("totalAmount", booking.getTotalAmount());

        return "customer/select-fnb";
    }


    // THÊM/SỬA/XÓA BẮP NƯỚC

    @PostMapping("/add")
    public String addFnB(@RequestParam Long bookingId,
                         @RequestParam Long itemId,
                         @RequestParam int quantity) {
        bookingFnBService.addFnBToBooking(bookingId, itemId, quantity);
        return "redirect:/customer/booking/" + bookingId + "/fnb";
    }

    @PostMapping("/remove")
    public String removeFnB(@RequestParam Long bookingId,
                            @RequestParam Long bookingFnbId) {
        bookingFnBService.removeFnBFromBooking(bookingFnbId);
        return "redirect:/customer/booking/" + bookingId + "/fnb";
    }

    @PostMapping("/update")
    public String updateFnBQuantity(@RequestParam Long bookingId,
                                    @RequestParam Long bookingFnbId,
                                    @RequestParam int quantity) {
        if (quantity <= 0) {
            bookingFnBService.removeFnBFromBooking(bookingFnbId);
        } else {
            bookingFnBService.updateFnBQuantity(bookingFnbId, quantity);
        }
        return "redirect:/customer/booking/" + bookingId + "/fnb";
    }
}