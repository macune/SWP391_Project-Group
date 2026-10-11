package fu.cinema.exception;

public class EmailAlreadyExistsException extends AppException {
    public EmailAlreadyExistsException() {super(ErrorCode.EMAIL_ALREADY_EXISTS);}
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
    public EmailAlreadyExistsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
