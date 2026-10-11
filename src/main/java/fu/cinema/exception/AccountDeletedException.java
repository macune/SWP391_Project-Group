package fu.cinema.exception;

public class AccountDeletedException extends AppException {
    public AccountDeletedException() {
        super(ErrorCode.ACCOUNT_HAS_DELETED);
    }
    public AccountDeletedException(ErrorCode errorCode) {
        super(errorCode);
    }
    public AccountDeletedException(String message) {
        super(message);
    }
}
