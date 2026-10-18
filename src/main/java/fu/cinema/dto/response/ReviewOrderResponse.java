package fu.cinema.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewOrderResponse {
    Long bookingId;
    String movieTitle;
    String posterUrl;
    String branchName;
    String hallName;
    LocalDateTime startTime;
    LocalDateTime endTime;

    // Phần vé xem phim
    List<String> seatNames;
    BigDecimal ticketTotal;

    // Phần f&b
    List<String> fnbDetails;
    BigDecimal fnbTotal;

    // Phần mã khuyến mãi
    String appliedPromoCode;
    BigDecimal discountAmount;

    // Tổng tiền phải trả
    BigDecimal finalAmount;
}
