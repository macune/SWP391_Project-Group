package fu.cinema.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketValidationResponse {

    // true nếu vé hợp lệ, đúng chi nhánh và đang ở trạng thái BOOKED (chưa sử dụng)
    private boolean validForCheckIn;

    // Các trạng thái: VALID, CHECKED_IN_SUCCESS, ALREADY_USED, WRONG_BRANCH, UNPAID, CANCELLED, NOT_FOUND
    private String validationState;

    // Thông báo chi tiết hiển thị cho nhân viên soát vé
    private String message;

    // Thông tin chi tiết của vé (Phim, Suất chiếu, Phòng, Ghế, Giá vé, Khách hàng...)
    private TicketResponse ticket;
}