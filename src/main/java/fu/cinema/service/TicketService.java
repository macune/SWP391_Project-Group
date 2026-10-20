package fu.cinema.service;

import fu.cinema.dto.request.TicketInitRequest;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.entity.Seat;
import fu.cinema.entity.Showtime;

import java.math.BigDecimal;
import java.util.List;

public interface TicketService {

    /**
     * Tính giá vé cho 1 ghế trong suất chiếu:
     * Giá vé = Showtime.basePrice + Seat.type.surcharge
     * (STANDARD: +0đ, VIP: +10.000đ, SWEETBOX: +50.000đ)
     */
    BigDecimal calculateTicketPrice(Showtime showtime, Seat seat);

    /**
     * Khởi tạo các bản ghi Ticket cho đơn hàng (Booking) từ danh sách seatIds.
     * Hỗ trợ tạo ở trạng thái LOCKED (khi mới giữ ghế) hoặc BOOKED (khi thanh toán ngay).
     */
    List<TicketResponse> initializeTickets(TicketInitRequest request);

    /**
     * Phát hành vé điện tử (E-Ticket & QR Code) sau khi thanh toán thành công:
     * - Trường hợp A: Nếu Booking đã có sẵn các bản ghi Ticket (đang ở trạng thái LOCKED),
     *   hàm sẽ cập nhật trạng thái sang BOOKED, chuẩn hóa giá vé và sinh mã QR chính thức.
     * - Trường hợp B: Nếu Booking chưa có bản ghi Ticket nào và truyền kèm showtimeId + seatIds,
     *   hàm sẽ tạo mới hoàn toàn các bản ghi Ticket ở trạng thái BOOKED kèm mã QR.
     */
    List<TicketResponse> generateETicketsForBooking(Long bookingId);

    List<TicketResponse> generateETicketsForBooking(Long bookingId, Long showtimeId, List<Long> seatIds);

    /**
     * Xem chi tiết 1 vé điện tử theo ticketId (có kiểm tra quyền sở hữu của Customer hoặc Staff/Admin).
     */
    TicketResponse getTicketDetail(Long ticketId, String currentUsername);

    /**
     * Xem danh sách toàn bộ vé điện tử thuộc 1 đơn đặt vé (Booking).
     */
    List<TicketResponse> getTicketsByBooking(Long bookingId, String currentUsername);

    /**
     * Lấy danh sách tất cả vé của khách hàng đang đăng nhập (Trang "Vé của tôi").
     */
    List<TicketResponse> getMyTickets(String username);

    /**
     * Quét mã QR / Nhập mã định danh để kiểm tra tính hợp lệ và hiển thị thông tin vé cho Staff.
     */
    fu.cinema.dto.response.TicketValidationResponse validateTicketByQr(String qrCode, String staffUsername);

    /**
     * Xác nhận soát vé cho khách vào phòng chiếu (Chuyển trạng thái BOOKED -> CHECKED_IN).
     */
    fu.cinema.dto.response.TicketValidationResponse confirmTicketCheckIn(Long ticketId, String staffUsername);
}