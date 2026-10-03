package fu.cinema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Tắt hoàn toàn CSRF (cần thiết cho quá trình code test không có form login hoàn chỉnh)
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập toàn bộ các trang giao diện (css, js, images)
                        .requestMatchers("/css/**", "/js/**", "/vendor/**", "/images/**").permitAll()

                        // Mở cửa toàn bộ cho module phim
                        .requestMatchers("/movies/**").permitAll()

                        // Các request khác tạm thời cũng cho phép (để bạn code dễ dàng)
                        // Sau này làm xong tính năng Login mới đổi thành .authenticated()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}