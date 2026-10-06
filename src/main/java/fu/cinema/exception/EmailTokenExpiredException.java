package fu.cinema.exception;

public class EmailTokenExpiredException extends AppException {
    public EmailTokenExpiredException() {
        super(ErrorCode.EMAIL_TOKEN_EXPIRED);
    }
    public EmailTokenExpiredException(ErrorCode errorCode) {
        super(errorCode);
    }
    public EmailTokenExpiredException(String message) {
        super(message);
    }
}
