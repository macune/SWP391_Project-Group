package fu.cinema.service;

import fu.cinema.dto.request.SeatLockRequest;
import fu.cinema.dto.response.*;
import fu.cinema.entity.Booking;
import fu.cinema.enums.BookingStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

public interface BookingService {
    // Lấy dữ liệu toàn bộ ghế trong 1 showtime
    SeatSelectionResponse getSeatSelectionData(Long showtimeId);

    // Khóa ghế theo showtimeId
    Long lockSeats(SeatLockRequest request);

    // Giải phóng ghế khi đơn hàng hết thời gian chờ
    void releaseExpiredSeats();

    // Lấy dữ liệu order để hiển thị màn hình
    ReviewOrderResponse getReviewOrderData(Long bookingId);

    // Áp dụng mã khuyến mãi vào đơn hàng
    void applyPromotion(Long bookingId, String promoCode);

    // Sử dụng khi khách hủy đặt vé -> Giải phóng ghế + xóa booking
    void cancelBooking(Long bookingId);

    BookDetailsResponse getBookingDetail(Long bookingId);

    // Staff: Đổi trạng thái đơn (Xác nhận hoặc Hủy đơn)
    void updateBookingStatus(Long bookingId, BookingStatus newStatus);

    // Lịch sử vé người dùng
    List<BookingHistoryResponse> getMyBookings(String keyword, BookingStatus status);

    //
    void confirmBooking(Long bookingId);
}

