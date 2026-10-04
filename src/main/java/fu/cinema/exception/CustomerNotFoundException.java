package fu.cinema.exception;

public class CustomerNotFoundException extends AppException {

    public CustomerNotFoundException() {
        super(ErrorCode.CUSTOMER_NOT_FOUND);
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
