package fu.cinema.dto.response;

import fu.cinema.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDetailsResponse {
    Long bookingId;
    String customerName;
    String customerPhone;
    String movieTitle;
    String hallName;
    LocalDateTime startTime;
    String seatNames;
    BigDecimal totalAmount;
    BookingStatus status;
}
