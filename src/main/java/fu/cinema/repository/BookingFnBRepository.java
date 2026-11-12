package fu.cinema.repository;

import fu.cinema.entity.BookingFnB;
import fu.cinema.enums.PaymentMethod;
import fu.cinema.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingFnBRepository extends JpaRepository<BookingFnB, Long> {

    List<BookingFnB> findByBooking_BookingId(Long bookingId);

    @Query(value = """
        SELECT bf FROM BookingFnB bf
        JOIN bf.booking b
        JOIN b.payment p
        WHERE bf.fnbItem.itemId = :itemId
          AND p.status = :status
          AND (:branchId IS NULL OR EXISTS (
                SELECT 1 FROM Ticket tk
                JOIN tk.showtime s
                JOIN s.hall h
                WHERE tk.booking = b AND h.branch.branchId = :branchId
          ))
          AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod)
          AND (:startTime IS NULL OR p.transactionTime >= :startTime)
          AND (:endTime IS NULL OR p.transactionTime < :endTime)
        """, countQuery = """
        SELECT COUNT(bf) FROM BookingFnB bf
        JOIN bf.booking b
        JOIN b.payment p
        WHERE bf.fnbItem.itemId = :itemId
          AND p.status = :status
          AND (:branchId IS NULL OR EXISTS (
                SELECT 1 FROM Ticket tk
                JOIN tk.showtime s
                JOIN s.hall h
                WHERE tk.booking = b AND h.branch.branchId = :branchId
          ))
          AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod)
          AND (:startTime IS NULL OR p.transactionTime >= :startTime)
          AND (:endTime IS NULL OR p.transactionTime < :endTime)
        """)
    Page<BookingFnB> findFnbTransactions(
            @Param("itemId") Long itemId,
            @Param("status") PaymentStatus status,
            @Param("branchId") Long branchId,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
}
