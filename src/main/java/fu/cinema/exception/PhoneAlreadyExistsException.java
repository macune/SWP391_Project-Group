package fu.cinema.exception;

public class PhoneAlreadyExistsException extends AppException {
    public PhoneAlreadyExistsException() {
        super(ErrorCode.PHONE_ALREADY_EXISTS);
    }
    public PhoneAlreadyExistsException(String message) {
        super(message);
    }
    public PhoneAlreadyExistsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
