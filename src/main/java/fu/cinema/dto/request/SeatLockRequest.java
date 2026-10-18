package fu.cinema.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLockRequest {
    @NotNull(message = "Không tìm thấy thông tin suất chiếu!")
    private Long showtimeId;
    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế!")
    private List<Long> seatIds;
}
