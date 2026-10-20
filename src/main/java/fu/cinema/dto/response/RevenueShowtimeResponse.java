
package fu.cinema.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueShowtimeResponse(
        Long showtimeId,
        String movieTitle,
        String hallName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long ticketCount,
        BigDecimal revenue,
        long totalSeats
) {
}
