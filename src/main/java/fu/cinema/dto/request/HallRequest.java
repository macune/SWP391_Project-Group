package fu.cinema.dto.request;

import fu.cinema.enums.HallStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallRequest {

    private Long hallId;

    @NotBlank(message = "Tên phòng chiếu không được để trống")
    private String hallName;

    @NotNull(message = "Vui lòng nhập số hàng ghế")
    @Min(value = 1, message = "Số hàng ghế tối thiểu là 1")
    @Max(value = 26, message = "Số hàng ghế tối đa là 26 (A - Z)")
    private Integer totalRows;

    @NotNull(message = "Vui lòng nhập số ghế mỗi hàng")
    @Min(value = 1, message = "Số ghế mỗi hàng tối thiểu là 1")
    @Max(value = 30, message = "Số ghế mỗi hàng tối đa là 30")
    private Integer seatsPerRow;

    private HallStatus status;
}