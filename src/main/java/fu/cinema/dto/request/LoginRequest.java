package fu.cinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;

    boolean rememberMe;
}
