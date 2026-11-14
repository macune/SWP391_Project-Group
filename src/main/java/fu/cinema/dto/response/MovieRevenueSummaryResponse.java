package fu.cinema.dto.response;

import java.math.BigDecimal;

public record MovieRevenueSummaryResponse(
        BigDecimal totalRevenue,
        long totalShowtimes,
        long totalTickets,
        long totalSeats
) {
}
