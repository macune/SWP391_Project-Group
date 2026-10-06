package fu.cinema.dto.request;

import fu.cinema.enums.SeatStatus;
import fu.cinema.enums.SeatType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatUpdateItem {
    private Long seatId;
    private SeatType type;
    private SeatStatus status;
}