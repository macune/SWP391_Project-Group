package fu.cinema.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLockResponse {
    private boolean isLockSuccess;
    private String message;
    private Long bookingId;
    private LocalDateTime expiresAt;
}
