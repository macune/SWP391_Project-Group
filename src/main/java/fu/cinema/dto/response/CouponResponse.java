package fu.cinema.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private boolean valid;
    private String message;
    private Integer discountPercentage;
    private BigDecimal maxDiscountAmount;
}