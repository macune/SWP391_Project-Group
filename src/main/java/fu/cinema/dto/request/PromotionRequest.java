package fu.cinema.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PromotionRequest {
    private Long id;

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    @NotNull(message = "Vui lòng nhập phần trăm giảm giá")
    @Min(value = 0, message = "Phần trăm giảm không được âm")
    private Integer discountPercentage;

    private String discountType = "PERCENTAGE";

    @NotNull(message = "Vui lòng nhập số tiền giảm tối đa")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") // Hứng đúng chuẩn thẻ <input type="datetime-local">
    private LocalDateTime validFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiryDate;

    private Boolean isActive;
}