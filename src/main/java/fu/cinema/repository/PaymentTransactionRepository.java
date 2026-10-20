package fu.cinema.repository;

import fu.cinema.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionNo(String transactionNo);
    boolean existsByBooking_BookingId(Long bookingId);
}