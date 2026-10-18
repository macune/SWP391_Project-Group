package fu.cinema.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ShowtimeRevenueDetailResponse(
        Long showtimeId,
        BigDecimal ticketRevenue,
        BigDecimal fnbRevenue,
        BigDecimal totalRevenue,
        long ticketCount,
        List<RevenueTransactionResponse> transactions
) {
}