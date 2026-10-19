package fu.cinema.mapper;

import fu.cinema.dto.response.TicketResponse;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.TicketStatus;
import fu.cinema.util.QrCodeUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TicketMapper {

    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "bookingStatus", source = "booking.status")
    @Mapping(target = "bookingTime", source = "booking.bookingTime")
    @Mapping(target = "customerUsername", source = "booking.account.username")
    @Mapping(target = "customerEmail", source = "booking.account.email")
    @Mapping(target = "seatId", source = "seat.seatId")
    @Mapping(target = "rowCode", source = "seat.rowCode")
    @Mapping(target = "seatNumber", source = "seat.number")
    @Mapping(target = "seatType", source = "seat.type")
    @Mapping(target = "showtimeId", source = "showtime.showtimeId")
    @Mapping(target = "startTime", source = "showtime.startTime")
    @Mapping(target = "endTime", source = "showtime.endTime")
    @Mapping(target = "basePrice", source = "showtime.basePrice")
    @Mapping(target = "movieId", source = "showtime.movie.movieId")
    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "posterUrl", source = "showtime.movie.posterUrl")
    @Mapping(target = "duration", source = "showtime.movie.duration")
    @Mapping(target = "genre", source = "showtime.movie.genre")
    @Mapping(target = "language", source = "showtime.movie.language")
    @Mapping(target = "ageRating", source = "showtime.movie.ageRating")
    @Mapping(target = "hallId", source = "showtime.hall.hallId")
    @Mapping(target = "hallName", source = "showtime.hall.hallName")
    @Mapping(target = "branchId", source = "showtime.hall.branch.branchId")
    @Mapping(target = "branchName", source = "showtime.hall.branch.branchName")
    @Mapping(target = "branchAddress", source = "showtime.hall.branch.address")
    @Mapping(target = "branchHotline", source = "showtime.hall.branch.hotline")
    @Mapping(target = "seatLabel", ignore = true)
    @Mapping(target = "seatTypeDisplay", ignore = true)
    @Mapping(target = "seatSurcharge", ignore = true)
    @Mapping(target = "qrCodeBase64", ignore = true)
    TicketResponse toTicketResponse(Ticket ticket);

    @AfterMapping
    default void enrichTicketDetails(Ticket ticket, @MappingTarget TicketResponse response) {
        if (ticket.getSeat() != null) {
            response.setSeatLabel(ticket.getSeat().getRowCode() + ticket.getSeat().getNumber());
            if (ticket.getSeat().getType() != null) {
                response.setSeatTypeDisplay(ticket.getSeat().getType().getDisplayName());
                response.setSeatSurcharge(ticket.getSeat().getType().getSurcharge());
            }
        }
        // Sinh ảnh QR Code Base64 cho vé BOOKED hoặc CHECKED_IN
        if (ticket.getStatus() == TicketStatus.BOOKED || ticket.getStatus() == TicketStatus.CHECKED_IN) {
            response.setQrCodeBase64(QrCodeUtil.generateQrCodeBase64(ticket.getQrCode(), 260, 260));
        }
    }
}