package fu.cinema.exception;

public class AccountNotFoundException extends AppException {
  public AccountNotFoundException() {
    super(ErrorCode.ACCOUNT_NOT_FOUND);
  }
    public AccountNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public AccountNotFoundException(String message) {
        super(message);
    }
}
