package fu.cinema.dto.response;

import fu.cinema.enums.ShowtimeStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeResponse {
    private Long showtimeId;
    private Long movieId;
    private String movieTitle;
    private Long hallId;
    private String hallName;
    private Long branchId;
    private String branchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private Integer hallCapacity;
    private Integer availableSeats;
}