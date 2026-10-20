package fu.cinema.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    CUSTOMER_ALREADY_EXISTS("Thông tin khách hàng đã tồn tại trong hệ thống."),
    USERNAME_ALREADY_EXISTS("Tên đăng nhập này đã có người sử dụng. Vui lòng chọn tên khác!"),
    EMAIL_ALREADY_EXISTS("Địa chỉ email này đã được sử dụng. Vui lòng nhập email khác!"),
    PHONE_ALREADY_EXISTS("Số điện thoại này đã được sử dụng. Vui lòng nhập số khác!"),
    CUSTOMER_NOT_FOUND("Không tìm thấy thông tin khách hàng trong hệ thống."),
    EMAIL_TOKEN_NOT_FOUND("Liên kết xác thực không hợp lệ hoặc không tồn tại!"),
    EMAIL_TOKEN_EXPIRED("Liên kết xác thực đã hết hạn. Vui lòng thử lại!"),
    UNCATEGORIZED_ERROR("Đã xảy ra sự cố trong hệ thống. Vui lòng thử lại sau!"),
    BRANCH_NOT_FOUND("Không tìm thấy thông tin chi nhánh trong hệ thống."),
    PASSWORD_NOT_MATCH("Mật khẩu chưa khớp."),
    STAFF_NOT_FOUND("Không tìm thấy thông tin nhân viên trong hệ thống."),
    ACCOUNT_HAS_DELETED("Tài khoản không tồn tại hoặc đã bị vô hiệu hóa"),
    ACCOUNT_NOT_FOUND("Không tìm thấy tài khoản."),
    SHOWTIME_NOT_FOUND("Không tìm thấy suất chiếu."),
    BOOKING_NOT_FOUND("Không tìm thấy đơn hàng."),
    PROMO_NOT_FOUND("Không tìm thấy mã khuyến mãi.");
    ACCOUNT_NOT_FOUND("Không tìm thấy tài khoản."),

    // Mã lỗi cho chức năng Đặt vé & Khởi tạo vé điện tử (FE-06.1)
    BOOKING_NOT_FOUND("Không tìm thấy đơn đặt vé trong hệ thống."),
    BOOKING_INVALID_STATUS("Đơn đặt vé đã bị hủy hoặc hết hạn, không thể phát hành vé điện tử."),
    SHOWTIME_NOT_FOUND("Không tìm thấy thông tin suất chiếu."),
    SEAT_NOT_FOUND("Không tìm thấy thông tin ghế ngồi."),
    SEAT_ALREADY_TAKEN("Một hoặc nhiều ghế bạn chọn đã có người đặt trong suất chiếu này."),
    TICKET_NOT_FOUND("Không tìm thấy thông tin vé điện tử."),
    TICKET_ACCESS_DENIED("Bạn không có quyền xem thông tin vé này."),
    QR_GENERATION_FAILED("Không thể khởi tạo hình ảnh mã QR cho vé điện tử."),

    // Mã lỗi cho chức năng Soát vé QR tại phòng chiếu (FE-06.2)
    TICKET_ALREADY_CHECKED_IN("Vé này đã được sử dụng (đã soát vé vào phòng trước đó)."),
    TICKET_NOT_BOOKED("Vé chưa hoàn tất thanh toán hoặc đã bị hủy, không thể vào phòng chiếu."),
    TICKET_WRONG_BRANCH("Vé này thuộc chi nhánh khác, không thể soát vé tại rạp hiện tại.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}