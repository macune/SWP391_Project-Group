package fu.cinema.exception;

public class CustomerAlreadyExistsException extends AppException {

    public CustomerAlreadyExistsException() {
        super(ErrorCode.CUSTOMER_ALREADY_EXISTS);
    }

    public CustomerAlreadyExistsException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}
