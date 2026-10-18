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

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Thống kê Top 5 phim có tổng doanh thu vé cao nhất
    @Query("SELECT t.showtime.movie.movieId " +
            "FROM Ticket t " +
            "WHERE t.status = 'SOLD' OR t.status = 'CHECKED_IN' " + // Tính các vé đã bán
            "GROUP BY t.showtime.movie.movieId " +
            "ORDER BY SUM(t.price) DESC")
    List<Long> findTop5MoviesByRevenue(Pageable pageable);
    // Đếm vé sắp chiếu của khách hàng
    long countByBooking_Account_UsernameAndBooking_StatusAndStatusInAndShowtime_StartTimeAfter(
            String username,
            BookingStatus bookingStatus,
            Collection<TicketStatus> statuses,
            LocalDateTime now
    );

    // Lịch sử vé, 9 vé mỗi trang

    @Query("""
    SELECT t
    FROM Ticket t
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


    // Top phim theo số vé được đặt
    @Query("""
    SELECT t.showtime.movie.movieId, COUNT(t.ticketId)
    FROM Ticket t
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
    Page<Ticket> findByBooking_Account_UsernameAndStatusNotOrderByBooking_BookingTimeDesc(
            String username,
            TicketStatus excludedStatus,
            Pageable pageable
    );
}
