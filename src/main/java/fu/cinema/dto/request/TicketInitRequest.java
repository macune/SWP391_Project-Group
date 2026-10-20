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
public class TicketInitRequest {

    @NotNull(message = "Mã đơn đặt vé không được để trống")
    private Long bookingId;

    @NotNull(message = "Mã suất chiếu không được để trống")
    private Long showtimeId;

    @NotEmpty(message = "Danh sách ghế chọn không được để trống")
    private List<Long> seatIds;

    // Nếu true: Khởi tạo trực tiếp ở trạng thái BOOKED kèm mã QR chính thức.
    // Nếu false: Khởi tạo ở trạng thái LOCKED (giữ ghế chờ thanh toán).
    @Builder.Default
    private boolean issueImmediately = false;
}