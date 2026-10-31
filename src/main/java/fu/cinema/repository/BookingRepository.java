package fu.cinema.repository;

import fu.cinema.entity.Booking;
import fu.cinema.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus bookingStatus, LocalDateTime time);

    List<Booking> findByAccount_AccountIdOrderByCreatedAtDesc(Long accountId);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByAccount_UsernameAndStatus(String username, BookingStatus status);

    List<Booking> findByStatusAndBookingTimeBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);

    // 1. Hàm 3 tham số (Dùng cho PromotionServiceImpl lúc khách ấn kiểm tra mã)
    boolean existsByAccount_AccountIdAndPromotion_IdAndStatusNot(
            Long accountId,
            Long promotionId,
            BookingStatus status
    );

    // 2. Hàm 4 tham số (Dành riêng cho Controller lúc lưu mã vào đơn hàng, bỏ qua đơn hiện tại)
    boolean existsByAccount_AccountIdAndPromotion_IdAndStatusNotAndBookingIdNot(
            Long accountId,
            Long promotionId,
            BookingStatus status,
            Long currentBookingId
    );
}