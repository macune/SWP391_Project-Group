package fu.cinema.exception;

public class UsernameAlreadyExistsException extends AppException {
    public UsernameAlreadyExistsException() {super(ErrorCode.USERNAME_ALREADY_EXISTS);}
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
    public UsernameAlreadyExistsException(ErrorCode errorCode) {super(errorCode);}
}
