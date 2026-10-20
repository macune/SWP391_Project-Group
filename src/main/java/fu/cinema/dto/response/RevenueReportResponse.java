
package fu.cinema.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RevenueReportResponse(
        BigDecimal totalRevenue,
        BigDecimal ticketRevenue,
        BigDecimal fnbRevenue,
        long transactionCount,
        List<RevenueTransactionResponse> transactions
) {
}
