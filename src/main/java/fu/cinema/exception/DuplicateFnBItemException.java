package fu.cinema.exception;

public class DuplicateFnBItemException  extends RuntimeException {
    public DuplicateFnBItemException(String name) {
        super("FnB Item:" + name + " already exists");
    }
}
