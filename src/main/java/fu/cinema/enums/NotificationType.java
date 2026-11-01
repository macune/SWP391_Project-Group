package fu.cinema.enums;

public enum NotificationType {
    BOOKING,    // FE-10.1: Thông báo Đặt vé (Xác nhận, Hủy, Cập nhật trạng thái)
    PAYMENT,    // FE-10.2: Thông báo Thanh toán (Thành công, Thất bại)
    SHOWTIME,   // FE-10.3: Thông báo Suất chiếu (Nhắc giờ chiếu 2h, Đổi lịch, Hủy lịch)
    PROMOTION   // FE-10.4: Thông báo Khuyến mãi & Mã giảm giá
}