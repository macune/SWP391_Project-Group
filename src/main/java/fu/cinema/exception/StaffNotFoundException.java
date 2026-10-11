package fu.cinema.exception;

public class StaffNotFoundException extends AppException {
    public StaffNotFoundException() {
        super(ErrorCode.STAFF_NOT_FOUND);
    }
    public StaffNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public StaffNotFoundException(String message) {
        super(message);
    }
}
