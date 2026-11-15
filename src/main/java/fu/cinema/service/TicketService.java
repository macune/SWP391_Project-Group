package fu.cinema.service;

import fu.cinema.dto.request.TicketInitRequest;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.dto.response.TicketValidationResponse;
import fu.cinema.entity.Seat;
import fu.cinema.entity.Showtime;

import java.math.BigDecimal;
import java.util.List;

public interface TicketService {

    BigDecimal calculateTicketPrice(Showtime showtime, Seat seat);

    List<TicketResponse> initializeTickets(TicketInitRequest request);

    List<TicketResponse> generateETicketsForBooking(Long bookingId);

    List<TicketResponse> generateETicketsForBooking(Long bookingId, Long showtimeId, List<Long> seatIds);

    TicketResponse getTicketDetail(Long ticketId, String currentUsername);

    List<TicketResponse> getTicketsByBooking(Long bookingId, String currentUsername);

    List<TicketResponse> getMyTickets(String username);

    TicketValidationResponse validateTicketByQr(String qrCode, String staffUsername);

    TicketValidationResponse confirmTicketCheckIn(Long ticketId, String staffUsername);

    List<TicketResponse> getMyActiveTickets(String username);
}