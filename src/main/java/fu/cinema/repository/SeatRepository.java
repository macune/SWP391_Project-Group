package fu.cinema.repository;

import fu.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByHallHallIdOrderByRowCodeAscNumberAsc(Long hallId);

    void deleteByHallHallId(Long hallId);
}