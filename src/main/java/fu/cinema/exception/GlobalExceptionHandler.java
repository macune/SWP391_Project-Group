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

    // 1. Lỗi nhập sai dữ liệu Phim (của thành viên làm Movie)
    @ExceptionHandler(InvalidMovieDataException.class)
    public String handleInvalidMovieData(InvalidMovieDataException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/movies");
    }

    // 2. Bắt các lỗi nghiệp vụ kế thừa từ AppException (của phần Auth/Customer)
    @ExceptionHandler(AppException.class)
    public ModelAndView handleAppException(AppException ex) {
        log.warn("Lỗi nghiệp vụ: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("common/error");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    // 3. Lỗi IllegalArgumentException (của Movie, Branch, Hall)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/movies");
    }

    // 4. Bắt tất cả các lỗi hệ thống còn lại (Exception)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        log.error("Lỗi hệ thống: ", ex);
        ModelAndView mav = new ModelAndView("common/error");
        // In rõ tên lỗi và chi tiết lỗi để dễ debug khi ghép nhánh
        mav.addObject("errorTitle", ex.getClass().getSimpleName());
        mav.addObject("errorMessage", "Chi tiết lỗi: " + ex.getMessage());
        return mav;
    }
}
