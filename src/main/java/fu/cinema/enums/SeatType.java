package fu.cinema.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum SeatType {
    STANDARD(BigDecimal.ZERO, "Ghế Thường"),
    VIP(new BigDecimal("10000"), "Ghế VIP"),
    SWEETBOX(new BigDecimal("50000"), "Ghế Đôi (Sweetbox)");

    private final BigDecimal surcharge;
    private final String displayName;
}