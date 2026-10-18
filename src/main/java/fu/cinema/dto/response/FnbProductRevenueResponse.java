package fu.cinema.dto.response;

import java.math.BigDecimal;

public record FnbProductRevenueResponse(
        Long itemId,
        String itemName,
        String category,
        long soldQuantity,
        BigDecimal revenue
) {
}