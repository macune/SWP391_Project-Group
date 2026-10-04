package fu.cinema.controller;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.dto.request.LoginRequest;
import fu.cinema.entity.Customer;
import fu.cinema.exception.CustomerAlreadyExistsException;
import fu.cinema.exception.ErrorCode;
import fu.cinema.security.JwtAuthenticationFilter;
import fu.cinema.security.JwtService;
import fu.cinema.service.CustomerService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ConcurrentModel();
    }

    @Test
    void testShowLoginForm() {
        String view = authController.showLoginForm(model);
        assertEquals("common/login", view);
        assertTrue(model.containsAttribute("loginRequest"));
    }

    @Test
    void testProcessLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .username("customer1")
                .password("password123")
                .rememberMe(true)
                .build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "loginRequest");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User userDetails = new User(
                "customer1",
                "hashed",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken("customer1", "ROLE_CUSTOMER")).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        String view = authController.processLogin(request, bindingResult, response, model);

        assertEquals("redirect:/", view);
        Cookie cookie = response.getCookie(JwtAuthenticationFilter.JWT_COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals("mock-jwt-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    void testProcessLoginBadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .username("customer1")
                .password("wrongpassword")
                .build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "loginRequest");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        String view = authController.processLogin(request, bindingResult, response, model);

        assertEquals("common/login", view);
        assertEquals("Tên đăng nhập hoặc mật khẩu không chính xác!", model.getAttribute("errorMessage"));
    }

    @Test
    void testLogout() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = authController.logout(response);

        assertEquals("redirect:/login?logout=true", view);
        Cookie cookie = response.getCookie(JwtAuthenticationFilter.JWT_COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals(0, cookie.getMaxAge());
    }

    @Test
    void testShowRegisterForm() {
        String view = authController.showRegisterForm(model);
        assertEquals("customer/register", view);
        assertTrue(model.containsAttribute("customerCreationRequest"));
    }

    @Test
    void testProcessRegisterSuccess() {
        CustomerCreationRequest request = CustomerCreationRequest.builder()
                .username("newcustomer")
                .password("123456")
                .confirmPassword("123456")
                .fullName("Khách Hàng Mới")
                .email("new@cinema.com")
                .phone("0987654321")
                .build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "customerCreationRequest");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(customerService.register(request)).thenReturn(new Customer());

        String view = authController.processRegister(request, bindingResult, model, redirectAttributes);

        assertEquals("redirect:/login", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    void testProcessRegisterPasswordMismatch() {
        CustomerCreationRequest request = CustomerCreationRequest.builder()
                .username("newcustomer")
                .password("123456")
                .confirmPassword("654321")
                .build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "customerCreationRequest");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = authController.processRegister(request, bindingResult, model, redirectAttributes);

        assertEquals("customer/register", view);
        assertTrue(bindingResult.hasFieldErrors("confirmPassword"));
        verify(customerService, never()).register(any());
    }

    @Test
    void testProcessRegisterExistingUsername() {
        CustomerCreationRequest request = CustomerCreationRequest.builder()
                .username("existinguser")
                .password("123456")
                .confirmPassword("123456")
                .build();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "customerCreationRequest");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(customerService.register(request)).thenThrow(new CustomerAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS));

        String view = authController.processRegister(request, bindingResult, model, redirectAttributes);

        assertEquals("customer/register", view);
        assertTrue(bindingResult.hasFieldErrors("username"));
    }
}
