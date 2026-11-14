
package fu.cinema.dto.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record RevenueReportResponse(
        BigDecimal totalRevenue,
        BigDecimal ticketRevenue,
        BigDecimal fnbRevenue,
        long transactionCount,
        Page<RevenueTransactionResponse> transactions
) {
}
