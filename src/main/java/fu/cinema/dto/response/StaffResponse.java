package fu.cinema.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class StaffResponse {
    Long staffId;
    String username;
    String email;
    String fullName;
    String phone;
    Long branchId;
    String branchName;
}
