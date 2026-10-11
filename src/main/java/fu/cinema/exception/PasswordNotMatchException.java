package fu.cinema.exception;

public class PasswordNotMatchException extends AppException {
    public PasswordNotMatchException() {
        super(ErrorCode.PASSWORD_NOT_MATCH);
    }

    public PasswordNotMatchException(String message) {
        super(message);
    }

    public PasswordNotMatchException(ErrorCode errorCode) {
        super(errorCode);
    }
}
