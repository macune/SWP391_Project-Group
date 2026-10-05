package fu.cinema.controller;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.dto.request.LoginRequest;
import fu.cinema.exception.AppException;
import fu.cinema.exception.CustomerAlreadyExistsException;
import fu.cinema.exception.ErrorCode;
import fu.cinema.security.JwtAuthenticationFilter;
import fu.cinema.security.JwtService;
import fu.cinema.service.CustomerService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final CustomerService customerService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    // 1. Hiển thị form đăng nhập
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Nếu tk đã được xác thực + không phải Khách vãng lai thì về Home
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "common/login";
    }

    // 2. Xử lý đăng nhập bằng bảo mật chuẩn JWT
    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "common/login";
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Trích xuất vai trò (Role) của người dùng
            String role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_CUSTOMER");

            // Tạo JWT Token từ JwtService
            String jwtToken = jwtService.generateToken(userDetails.getUsername(), role);

            // Lưu JWT Token vào HttpOnly Cookie để trình duyệt bảo mật và tự động gửi kèm các request
            Cookie jwtCookie = new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, jwtToken);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(request.isRememberMe() ? (int) (jwtService.getExpirationTime() / 1000) : -1);
            response.addCookie(jwtCookie);

            // Lưu Authentication vào SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Phân quyền
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_MANAGER".equals(role)) {
                return "redirect:/manager/showtimes";
            } else if ("ROLE_STAFF".equals(role)) {
                return "redirect:/staff/pos";
            } else {
                return "redirect:/";
            }

        } catch (BadCredentialsException ex) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
            return "common/login";
        } catch (DisabledException ex) {
            model.addAttribute("errorMessage", "Tài khoản chưa được kích hoạt! Vui lòng kiểm tra email để xác thực tài khoản trước khi đăng nhập.");
            return "common/login";
        } catch (LockedException ex) {
            model.addAttribute("errorMessage", "Tài khoản của bạn đã bị khóa! Vui lòng liên hệ ban quản trị để được hỗ trợ.");
            return "common/login";
        } catch (AuthenticationException ex) {
            model.addAttribute("errorMessage", "Đăng nhập thất bại: " + ex.getMessage());
            return "common/login";
        }
    }

    // 3. Xử lý logout (Xóa JWT Cookie, Session và SecurityContext)
    @org.springframework.web.bind.annotation.RequestMapping(value = "/logout", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        Cookie jwtCookie = new Cookie(JwtAuthenticationFilter.JWT_COOKIE_NAME, null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request, response, auth);
        }
        SecurityContextHolder.clearContext();

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return "redirect:/login?logout=true";
    }

    // 4. Hiển thị form đăng ký khách hàng
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("customerCreationRequest")) {
            model.addAttribute("customerCreationRequest", new CustomerCreationRequest());
        }
        return "customer/register";
    }

    // 5. Xử lý gửi biểu mẫu đăng ký
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("customerCreationRequest") CustomerCreationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra khớp mật khẩu xác nhận
        if (request.getPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không trùng khớp!");
        }

        // Nếu có lỗi validate, giữ người dùng ở lại form đăng ký để hiển thị lỗi bên dưới input
        if (bindingResult.hasErrors()) {
            return "customer/register";
        }

        try {
            customerService.register(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng xác thực tài khoản thông qua Email vừa đăng ký.");
            return "redirect:/login";
        } catch (CustomerAlreadyExistsException ex) {
            // Đẩy lỗi vào đúng trường input tương ứng
            if (ex.getErrorCode() == ErrorCode.USERNAME_ALREADY_EXISTS) {
                bindingResult.rejectValue("username", "error.username", ex.getMessage());
            } else if (ex.getErrorCode() == ErrorCode.EMAIL_ALREADY_EXISTS) {
                bindingResult.rejectValue("email", "error.email", ex.getMessage());
            } else if (ex.getErrorCode() == ErrorCode.PHONE_ALREADY_EXISTS) {
                bindingResult.rejectValue("phone", "error.phone", ex.getMessage());
            } else {
                model.addAttribute("errorMessage", ex.getMessage());
            }
            return "customer/register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            customerService.verifyEmail(token);
            redirectAttributes.addFlashAttribute("successMessage", "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/login";
    }
}
