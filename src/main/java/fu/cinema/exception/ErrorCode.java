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
    UNCATEGORIZED_ERROR("Đã xảy ra sự cố trong hệ thống. Vui lòng thử lại sau!");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
