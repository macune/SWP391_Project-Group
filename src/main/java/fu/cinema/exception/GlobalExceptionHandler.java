package fu.cinema.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // lỗi nhập sai dữ liệu Phim
    @ExceptionHandler(InvalidMovieDataException.class)
    public String handleInvalidMovieData(InvalidMovieDataException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/movies");
    // Bắt các lỗi nghiệp vụ ( Bắt lỗi AppException, các lỗi con extends từ AppException cũng bị bắt)
    @ExceptionHandler(AppException.class)
    public ModelAndView handleAppException(AppException ex) {
        log.warn("Lỗi nghiệp vụ: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("common/error");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    // lỗi không tìm thấy Phim
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/movies";
    // Bắt tất cả các lỗi hệ thống (Exception)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        log.error("Lỗi hệ thống: ", ex);
        ModelAndView mav = new ModelAndView("common/error");
        mav.addObject("errorMessage", "Đã xảy ra sự cố trong quá trình xử lý. Vui lòng thử lại sau!");
        return mav;
    }
}
