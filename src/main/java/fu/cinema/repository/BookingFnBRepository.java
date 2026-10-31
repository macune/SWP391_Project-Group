package fu.cinema.repository;

import fu.cinema.entity.BookingFnB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingFnBRepository extends JpaRepository<BookingFnB,Long> {
    List<BookingFnB> findByBooking_BookingId(Long bookingId);
}
