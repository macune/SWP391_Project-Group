package fu.cinema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatSelectionResponse {
    private Long showtimeId;
    private String movieTitle;
    private String posterUrl;
    private String ageRating;
    private String branchName;
    private String hallName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;

    // Ma trận ghế theo hàng (Key là hàng và Value là các ghế trong hàng đó)
    private Map<String, List<BookingSeatResponse>> seatMap;
}
