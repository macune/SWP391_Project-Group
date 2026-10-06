package fu.cinema.dto.request;

import fu.cinema.enums.BranchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRequest {

    private Long branchId;

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String branchName;

    @NotBlank(message = "Địa chỉ chi nhánh không được để trống")
    private String address;

    @NotBlank(message = "Hotline không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{8,10}$", message = "Số hotline không hợp lệ (VD: 0901234567)")
    private String hotline;

    @NotNull(message = "Giờ mở cửa không được để trống")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @NotNull(message = "Giờ đóng cửa không được để trống")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closingTime;

    private BranchStatus status;
}