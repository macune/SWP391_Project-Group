package fu.cinema.dto.response;

import fu.cinema.enums.SeatType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeatResponse {
    private Long seatId;
    private String rowCode;
    private Integer number;
    private String seatName;
    private SeatType type;
    private BigDecimal price;
    private boolean isBooked;
    private boolean isBroken;
}
