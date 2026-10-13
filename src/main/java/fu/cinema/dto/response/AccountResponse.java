package fu.cinema.dto.response;

import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private Long accountId;
    private String username;
    private String email;
    private String phone;
    private Role role;
    private AccountStatus status;
}
