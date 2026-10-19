package fu.cinema.controller.staff;

import fu.cinema.dto.response.BookDetailsResponse;
import fu.cinema.enums.BookingStatus;
import fu.cinema.repository.BookingRepository;
import fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    // Xem danh sách (có hỗ trợ tìm kiếm và lọc)
    @GetMapping
    public String list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) BookingStatus status,
            Model model) {

        List<BookDetailsResponse> bookings = bookingService.getAllBookings(keyword, status);
        model.addAttribute("bookings", bookings);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status); // Giữ lại giá trị vừa chọn trên dropdown
        return "staff/booking-list";
    }

    // Xem chi tiết
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("booking", bookingService.getBookingDetail(id));
        return "staff/booking-details";
    }

    // Chỉnh sửa đơn đặt vé (Đổi trạng thái)
    @PostMapping("/{id}/edit")
    public String editBooking(@PathVariable Long id,
                              @RequestParam("status") BookingStatus status,
                              RedirectAttributes redirectAttributes) {
        bookingService.updateBookingStatus(id, status);

        if (status == BookingStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn #" + id + " và giải phóng ghế thành công!");
            return "redirect:/staff/bookings"; // Quay lại danh sách
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đơn #" + id + " thành công!");
        return "redirect:/staff/bookings/" + id;
    }
}