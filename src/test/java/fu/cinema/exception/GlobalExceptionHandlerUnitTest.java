package fu.cinema.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleAppExceptionWithErrorCode() {
        AppException ex = new AppException(ErrorCode.CUSTOMER_ALREADY_EXISTS);
        ModelAndView mav = exceptionHandler.handleAppException(ex);

        assertNotNull(mav);
        assertEquals("common/error", mav.getViewName());
        assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS.getMessage(), mav.getModel().get("errorMessage"));
    }

    @Test
    void testHandleAppExceptionWithCustomMessage() {
        AppException ex = new AppException("Lỗi tùy chỉnh khi đăng ký");
        ModelAndView mav = exceptionHandler.handleAppException(ex);

        assertNotNull(mav);
        assertEquals("common/error", mav.getViewName());
        assertEquals("Lỗi tùy chỉnh khi đăng ký", mav.getModel().get("errorMessage"));
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new RuntimeException("Lỗi hệ thống bất ngờ");
        ModelAndView mav = exceptionHandler.handleGenericException(ex);

        assertNotNull(mav);
        assertEquals("common/error", mav.getViewName());
        assertTrue(mav.getModel().containsKey("errorMessage"));
    }
}
