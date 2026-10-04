package fu.cinema.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Bắt các lỗi nghiệp vụ ( Bắt lỗi AppException, các lỗi con extends từ AppException cũng bị bắt)
    @ExceptionHandler(AppException.class)
    public ModelAndView handleAppException(AppException ex) {
        log.warn("Lỗi nghiệp vụ: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("common/error");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    // Bắt tất cả các lỗi hệ thống (Exception)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        log.error("Lỗi hệ thống: ", ex);
        ModelAndView mav = new ModelAndView("common/error");
        mav.addObject("errorMessage", "Đã xảy ra sự cố trong quá trình xử lý. Vui lòng thử lại sau!");
        return mav;
    }
}
