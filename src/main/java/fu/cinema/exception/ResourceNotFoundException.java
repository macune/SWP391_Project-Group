package fu.cinema.exception;

/**
 * Ngoại lệ khi không tìm thấy tài nguyên trong hệ thống.
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
