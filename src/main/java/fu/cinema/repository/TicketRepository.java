package fu.cinema.repository;

import fu.cinema.entity.Ticket;
import fu.cinema.enums.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Thống kê Top 5 phim có tổng doanh thu vé cao nhất
    @Query("SELECT t.showtime.movie.movieId " +
            "FROM Ticket t " +
            "WHERE t.status = 'BOOKED' OR t.status = 'CHECKED_IN' " +
            "GROUP BY t.showtime.movie.movieId " +
            "ORDER BY SUM(t.price) DESC")
    List<Long> findTop5MoviesByRevenue(Pageable pageable);

    // Lấy chi tiết 1 vé theo ticketId kèm đầy đủ thông tin Booking, Seat, Showtime, Movie, Hall, Branch
    @Query("SELECT t FROM Ticket t " +
            "JOIN FETCH t.booking b " +
            "JOIN FETCH b.account acc " +
            "JOIN FETCH t.seat s " +
            "JOIN FETCH t.showtime st " +
            "JOIN FETCH st.movie m " +
            "JOIN FETCH st.hall h " +
            "JOIN FETCH h.branch br " +
            "WHERE t.ticketId = :ticketId")
    Optional<Ticket> findByIdWithFullDetails(@Param("ticketId") Long ticketId);

    // Lấy chi tiết 1 vé theo mã qrCode (phục vụ xem vé & quét QR)
    @Query("SELECT t FROM Ticket t " +
            "JOIN FETCH t.booking b " +
            "JOIN FETCH b.account acc " +
            "JOIN FETCH t.seat s " +
            "JOIN FETCH t.showtime st " +
            "JOIN FETCH st.movie m " +
            "JOIN FETCH st.hall h " +
            "JOIN FETCH h.branch br " +
            "WHERE t.qrCode = :qrCode")
    Optional<Ticket> findByQrCodeWithFullDetails(@Param("qrCode") String qrCode);

    // Lấy toàn bộ danh sách vé của 1 đơn đặt vé (Booking) sắp xếp theo hàng ghế và số ghế
    @Query("SELECT t FROM Ticket t " +
            "JOIN FETCH t.booking b " +
            "JOIN FETCH b.account acc " +
            "JOIN FETCH t.seat s " +
            "JOIN FETCH t.showtime st " +
            "JOIN FETCH st.movie m " +
            "JOIN FETCH st.hall h " +
            "JOIN FETCH h.branch br " +
            "WHERE b.bookingId = :bookingId " +
            "ORDER BY s.rowCode ASC, s.number ASC")
    List<Ticket> findAllByBookingIdWithDetails(@Param("bookingId") Long bookingId);

    // Lấy danh sách tất cả các vé của 1 khách hàng theo username (mới nhất lên đầu)
    @Query("SELECT t FROM Ticket t " +
            "JOIN FETCH t.booking b " +
            "JOIN FETCH b.account acc " +
            "JOIN FETCH t.seat s " +
            "JOIN FETCH t.showtime st " +
            "JOIN FETCH st.movie m " +
            "JOIN FETCH st.hall h " +
            "JOIN FETCH h.branch br " +
            "WHERE acc.username = :username " +
            "ORDER BY b.bookingTime DESC, s.rowCode ASC, s.number ASC")
    List<Ticket> findAllByCustomerUsernameWithDetails(@Param("username") String username);

    // Kiểm tra ghế trong suất chiếu đã bị giữ/đặt hay chưa (bỏ qua vé đã CANCELLED)
    boolean existsByShowtime_ShowtimeIdAndSeat_SeatIdAndStatusNot(
            Long showtimeId, Long seatId, TicketStatus excludedStatus);
}