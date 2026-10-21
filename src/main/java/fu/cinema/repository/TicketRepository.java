package fu.cinema.repository;

import fu.cinema.entity.Ticket;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t.showtime.movie.movieId FROM Ticket t WHERE t.status = 'BOOKED' OR t.status = 'CHECKED_IN' GROUP BY t.showtime.movie.movieId ORDER BY SUM(t.price) DESC")
    List<Long> findTop5MoviesByRevenue(Pageable pageable);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b JOIN FETCH b.account acc JOIN FETCH t.seat s JOIN FETCH t.showtime st JOIN FETCH st.movie m JOIN FETCH st.hall h JOIN FETCH h.branch br WHERE t.ticketId = :ticketId")
    Optional<Ticket> findByIdWithFullDetails(@Param("ticketId") Long ticketId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b JOIN FETCH b.account acc JOIN FETCH t.seat s JOIN FETCH t.showtime st JOIN FETCH st.movie m JOIN FETCH st.hall h JOIN FETCH h.branch br WHERE t.qrCode = :qrCode")
    Optional<Ticket> findByQrCodeWithFullDetails(@Param("qrCode") String qrCode);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b JOIN FETCH b.account acc JOIN FETCH t.seat s JOIN FETCH t.showtime st JOIN FETCH st.movie m JOIN FETCH st.hall h JOIN FETCH h.branch br WHERE b.bookingId = :bookingId ORDER BY s.rowCode ASC, s.number ASC")
    List<Ticket> findAllByBookingIdWithDetails(@Param("bookingId") Long bookingId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b JOIN FETCH b.account acc JOIN FETCH t.seat s JOIN FETCH t.showtime st JOIN FETCH st.movie m JOIN FETCH st.hall h JOIN FETCH h.branch br WHERE acc.username = :username ORDER BY b.bookingTime DESC, s.rowCode ASC, s.number ASC")
    List<Ticket> findAllByCustomerUsernameWithDetails(@Param("username") String username);

    boolean existsByShowtime_ShowtimeIdAndSeat_SeatIdAndStatusNot(Long showtimeId, Long seatId, TicketStatus excludedStatus);

    @Query("SELECT t.seat.seatId FROM Ticket t WHERE t.showtime.showtimeId = :showtimeId AND t.status IN :statuses")
    List<Long> findBookedSeatIdsByShowtimeId(@Param("showtimeId") Long showtimeId, @Param("statuses") List<TicketStatus> statuses);

    long countByBooking_Account_UsernameAndBooking_StatusAndStatusInAndShowtime_StartTimeAfter(String username, BookingStatus bookingStatus, Collection<TicketStatus> statuses, LocalDateTime now);

    @Query("""
    SELECT t FROM Ticket t
    WHERE t.booking.account.username = :username
      AND t.status <> :excludedStatus
      AND (:showFrom IS NULL OR t.showtime.startTime >= :showFrom)
      AND (:showTo IS NULL OR t.showtime.startTime < :showTo)
      AND (:bookingFrom IS NULL OR t.booking.bookingTime >= :bookingFrom)
      AND (:bookingTo IS NULL OR t.booking.bookingTime < :bookingTo)
    ORDER BY t.booking.bookingTime DESC
    """)
    Page<Ticket> searchCustomerTickets(
            @Param("username") String username,
            @Param("excludedStatus") TicketStatus excludedStatus,
            @Param("showFrom") LocalDateTime showFrom,
            @Param("showTo") LocalDateTime showTo,
            @Param("bookingFrom") LocalDateTime bookingFrom,
            @Param("bookingTo") LocalDateTime bookingTo,
            Pageable pageable
    );

    @Query("""
    SELECT t.showtime.movie.movieId, COUNT(t.ticketId) FROM Ticket t
    WHERE t.booking.status = :bookingStatus
      AND t.status IN :ticketStatuses
      AND t.showtime.movie.status = :movieStatus
    GROUP BY t.showtime.movie.movieId
    ORDER BY COUNT(t.ticketId) DESC
    """)
    List<Object[]> findTopMoviesByBookingCount(
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("ticketStatuses") Collection<TicketStatus> ticketStatuses,
            @Param("movieStatus") MovieStatus movieStatus,
            Pageable pageable
    );

    Page<Ticket> findByBooking_Account_UsernameAndStatusNotOrderByBooking_BookingTimeDesc(String username, TicketStatus excludedStatus, Pageable pageable);
}