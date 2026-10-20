
package fu.cinema.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueTransactionResponse(
        Long paymentId,
        Long bookingId,
        LocalDateTime transactionTime,
        String paymentMethod,
        BigDecimal amount,
        BigDecimal ticketRevenue,
        BigDecimal fnbRevenue,
        long ticketCount
) {
}
