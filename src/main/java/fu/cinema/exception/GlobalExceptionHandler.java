package fu.cinema.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // lỗi nhập sai dữ liệu Phim
    @ExceptionHandler(InvalidMovieDataException.class)
    public String handleInvalidMovieData(InvalidMovieDataException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/movies");
    }

    // lỗi không tìm thấy Phim
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/movies";
    }
}