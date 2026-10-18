package fu.cinema.exception;

public class ShowtimeNotFoundException extends AppException {
  public ShowtimeNotFoundException() {
    super(ErrorCode.SHOWTIME_NOT_FOUND);
  }
    public ShowtimeNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public ShowtimeNotFoundException(String message) {
        super(message);
    }
}
