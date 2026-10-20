package fu.cinema.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCallbackResponse {
    private boolean success;
    private Long bookingId;
    private String transactionNo;
    private String message;
}