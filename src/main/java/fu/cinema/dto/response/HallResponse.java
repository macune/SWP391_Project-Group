package fu.cinema.dto.response;

import fu.cinema.enums.HallStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallResponse {
    private Long hallId;
    private Long branchId;
    private String branchName;
    private String hallName;
    private Integer capacity;
    private Integer totalRows;
    private Integer seatsPerRow;
    private Integer activeSeatCount;
    private HallStatus status;
}