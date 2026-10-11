package fu.cinema.repository;

import fu.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    @Query("SELECT s FROM Showtime s " +
            "WHERE s.movie.movieId = :movieId " +
            "AND (:branchId IS NULL OR s.hall.branch.branchId = :branchId) " +
            "AND s.startTime >= :now " +
            "ORDER BY s.startTime ASC")
    List<Showtime> findAvailableShowtimes(
            @Param("movieId") Long movieId,
            @Param("branchId") Long branchId,
            @Param("now") LocalDateTime now
    );
}