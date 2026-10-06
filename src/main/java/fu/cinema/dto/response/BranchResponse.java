package fu.cinema.dto.response;

import fu.cinema.enums.BranchStatus;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {
    private Long branchId;
    private String branchName;
    private String address;
    private String hotline;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private BranchStatus status;

    private Long managerId;
    private String managerFullName;
    private String managerUsername;
    private String managerEmail;
    private String managerPhone;
}