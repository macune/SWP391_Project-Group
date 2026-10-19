package fu.cinema.dto.response;

import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.SeatType;
import fu.cinema.enums.TicketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    // Thông tin định danh vé & QR Code
    private Long ticketId;
    private String qrCode;
    private String qrCodeBase64;
    private BigDecimal price;
    private TicketStatus status;
    private LocalDateTime checkedInAt;

    // Thông tin đơn đặt vé (Booking)
    private Long bookingId;
    private BookingStatus bookingStatus;
    private LocalDateTime bookingTime;
    private String customerUsername;
    private String customerEmail;

    // Thông tin ghế (Seat)
    private Long seatId;
    private String rowCode;
    private Integer seatNumber;
    private String seatLabel; // Ví dụ: A1, B12
    private SeatType seatType;
    private String seatTypeDisplay;
    private BigDecimal seatSurcharge;

    // Thông tin suất chiếu (Showtime)
    private Long showtimeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;

    // Thông tin phim (Movie)
    private Long movieId;
    private String movieTitle;
    private String posterUrl;
    private Integer duration;
    private String genre;
    private String language;
    private String ageRating;

    // Thông tin phòng chiếu & rạp (Hall & Branch)
    private Long hallId;
    private String hallName;
    private Long branchId;
    private String branchName;
    private String branchAddress;
    private String branchHotline;
}