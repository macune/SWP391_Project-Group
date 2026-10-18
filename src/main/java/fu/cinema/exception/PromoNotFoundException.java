package fu.cinema.exception;

public class PromoNotFoundException extends AppException {
    public PromoNotFoundException() {
        super(ErrorCode.PROMO_NOT_FOUND);
    }
    public PromoNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public PromoNotFoundException(String message) {
        super(message);
    }
}
