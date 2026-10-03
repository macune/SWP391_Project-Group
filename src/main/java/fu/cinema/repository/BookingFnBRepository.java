package fu.cinema.repository;

import fu.cinema.entity.BookingFnB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingFnBRepository extends JpaRepository<BookingFnB,Long> {
}
