package fu.cinema.repository;

import fu.cinema.entity.Ticket;
import fu.cinema.enums.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // Lấy danh sách ID các ghế đã được giữ chỗ/đã mua cho 1 suất chiếu
    @Query("SELECT t.seat.seatId FROM Ticket t " +
            "WHERE t.showtime.showtimeId = :showtimeId " +
            "AND t.status IN :statuses")
    List<Long> findBookedSeatIdsByShowtimeId(
            @Param("showtimeId") Long showtimeId,
            @Param("statuses") List<TicketStatus> statuses
    );
}