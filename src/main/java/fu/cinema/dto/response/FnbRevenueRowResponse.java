package fu.cinema.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FnbRevenueRowResponse(
        Long itemId,
        Long paymentId,
        LocalDateTime transactionTime,
        String paymentMethod,
        String category,
        String itemName,
        Integer quantity,
        BigDecimal subtotal
) {
}