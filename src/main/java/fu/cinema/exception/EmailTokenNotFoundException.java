package fu.cinema.exception;

public class EmailTokenNotFoundException extends AppException {
    public EmailTokenNotFoundException() {
        super(ErrorCode.EMAIL_TOKEN_NOT_FOUND);
    }
    public EmailTokenNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public EmailTokenNotFoundException(String message) {
        super(message);
    }
}
