package fu.cinema.exception;

public class BookingNotFoundException extends AppException {
    public BookingNotFoundException() {
        super(ErrorCode.BOOKING_NOT_FOUND);
    }
    public BookingNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public BookingNotFoundException(String message) {
        super(message);
    }
}
