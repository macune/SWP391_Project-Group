package fu.cinema.dto.response;

import fu.cinema.enums.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeAnalyticsRowResponse(
        String movieTitle,
        String hallName,
        String branchName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ShowtimeStatus status,
        long soldTickets,
        long capacity,
        double occupancy,
        BigDecimal revenue
) {
}
