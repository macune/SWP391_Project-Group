package fu.cinema.exception;

import lombok.Getter;

// AppException là lớp lỗi cha, tất cả các lỗi nghiệp vụ phải được kế thừa từ lớp này
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    // Cho phép truyền bằng Enums
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // Hoặc truyền bằng message tự gõ
    public AppException(String message) {
        super(message);
        this.errorCode = null;
    }
}
