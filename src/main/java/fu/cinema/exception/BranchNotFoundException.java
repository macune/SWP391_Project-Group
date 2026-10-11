package fu.cinema.exception;

public class BranchNotFoundException extends AppException {
    public BranchNotFoundException() {
        super(ErrorCode.BRANCH_NOT_FOUND);
    }
    public BranchNotFoundException(String message) {
        super(message);
    }
    public BranchNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
