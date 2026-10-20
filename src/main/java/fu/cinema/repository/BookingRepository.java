package fu.cinema.repository;

import fu.cinema.entity.Booking;
import fu.cinema.enums.BookingStatus;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus bookingStatus, LocalDateTime time);

    List<Booking> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);

    // Lấy toàn bộ đơn hàng sắp xếp theo thời gian tạo mới nhất
    List<Booking> findAllByOrderByCreatedAtDesc();
    long countByAccount_UsernameAndStatus(
            String username,
            BookingStatus status
    );
    List<Booking> findByStatusAndBookingTimeBetween(
            BookingStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
