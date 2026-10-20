
package fu.cinema.repository;

import fu.cinema.entity.Payment;
import fu.cinema.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
        SELECT DISTINCT p
        FROM Payment p
        JOIN p.booking b
        JOIN b.tickets t
        JOIN t.showtime s
        JOIN s.hall h
        WHERE p.status = :status
          AND h.branch.branchId = :branchId
          AND (:startTime IS NULL OR p.transactionTime >= :startTime)
          AND (:endTime IS NULL OR p.transactionTime < :endTime)
        ORDER BY p.transactionTime DESC
        """)
    List<Payment> findSuccessfulPaymentsByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("status") PaymentStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
    SELECT DISTINCT p
    FROM Payment p
    JOIN p.booking b
    JOIN b.tickets t
    JOIN t.showtime s
    JOIN s.hall h
    WHERE p.status = :status
      AND (:branchId IS NULL OR h.branch.branchId = :branchId)
      AND (:startTime IS NULL OR p.transactionTime >= :startTime)
      AND (:endTime IS NULL OR p.transactionTime < :endTime)
    ORDER BY p.transactionTime DESC
    """)
    List<Payment> findSuccessfulPaymentsForAdmin(
            @Param("branchId") Long branchId,
            @Param("status") PaymentStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
