package fu.cinema.config;

import fu.cinema.security.CustomerUserDetailsService;
import fu.cinema.security.JwtAuthenticationEntryPoint;
import fu.cinema.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomerUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Ngăn Tomcat đăng ký làm Servlet filter độc lập
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // 1. ACTOR GUEST / PUBLIC (Trang chủ, Auth, Báo lỗi, Static resources)
                        .requestMatchers(
                                "/", "/home", "/login", "/register", "/logout", "/verify-email", "/access-denied",
                                "/css/**", "/js/**", "/vendor/**", "/webjars/**", "/images/**", "/error"
                        ).permitAll()

                        // 2. ACTOR SYSTEM ADMIN (Quản trị hệ thống, Chi nhánh, Kho phim hệ thống)
                        .requestMatchers("/admin/**", "/movies/**").hasRole("ADMIN")

                        // 3. ACTOR BRANCH MANAGER (Quản lý rạp, Phòng chiếu, Lịch chiếu, Bắp nước)
                        .requestMatchers("/manager/**", "/api/fnb-items/**").hasAnyRole("ADMIN", "MANAGER")

                        // 4. ACTOR STAFF (Bán vé tại quầy POS, Soát vé QR)
                        .requestMatchers("/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")

                        // 5. ACTOR CUSTOMER / MEMBER (Đặt vé online, Lịch sử vé, Profile cá nhân)
                        .requestMatchers("/customer/**", "/booking/**", "/profile/**").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")

                        // Các request còn lại bắt buộc phải đăng nhập
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedPage("/access-denied")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies(JwtAuthenticationFilter.JWT_COOKIE_NAME, "JSESSIONID")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}