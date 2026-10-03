package fu.cinema.exception;

public class FnBItemNotFoundException extends RuntimeException {
    public FnBItemNotFoundException(Long id) {
        super("FnB Item not found with ID" + id);
    }
}
