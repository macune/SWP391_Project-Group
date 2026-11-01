package fu.cinema.dto.response;

import fu.cinema.enums.AccountStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Long customerId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private AccountStatus status;
}