package fu.cinema.dto.response;

import fu.cinema.enums.SeatStatus;
import fu.cinema.enums.SeatType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {
    private Long seatId;
    private String rowCode;
    private Integer number;
    private SeatType type;
    private SeatStatus status;
}