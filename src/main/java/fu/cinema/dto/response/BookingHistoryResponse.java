package fu.cinema.dto.response;

import fu.cinema.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingHistoryResponse {
    Long bookingId;
    String movieTitle;
    LocalDateTime showtime;
    String seats;
    BigDecimal totalAmount;
    BookingStatus status;
}
