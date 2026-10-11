package fu.cinema.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ShowtimeRequest {

    @NotNull(message = "Please select a movie")
    private Long movieId;

    @NotNull(message = "Please select a hall")
    private Long hallId;

    @NotNull(message = "Please select a showtime date")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate showDate;

    @NotNull(message = "Please select a start time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "Please enter the ticket price")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Ticket price must be greater than 0")
    private BigDecimal basePrice;

    // Các ngày muốn duplicate suất chiếu
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private List<LocalDate> DuplicateDates;
}