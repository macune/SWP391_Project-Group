package fu.cinema.repository;

import fu.cinema.entity.Showtime;
import fu.cinema.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    List<Showtime> findByHall_HallIdAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
            Long hallId,
            LocalDateTime endTime,
            LocalDateTime startTime,
            ShowtimeStatus status
    );

    List<Showtime> findByHall_Branch_BranchIdOrderByStartTimeAsc(Long branchId);

    List<Showtime> findByHall_HallIdAndStatusNot(Long hallId, ShowtimeStatus status);

    List<Showtime> findByHall_HallIdAndShowtimeIdNotAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
            Long hallId,
            Long showtimeId,
            LocalDateTime endTime,
            LocalDateTime startTime,
            ShowtimeStatus status
    );

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

    List<Showtime> findByMovie_MovieIdAndStartTimeAfterAndStatusNotOrderByStartTimeAsc(
            Long movieId,
            LocalDateTime now,
            ShowtimeStatus status
    );

    List<Showtime> findByHall_Branch_BranchIdAndStartTimeBetweenAndStatusNotOrderByStartTimeAsc(
            Long branchId,
            LocalDateTime start,
            LocalDateTime end,
            ShowtimeStatus status
    );
    @Modifying
    @Query("""
    UPDATE Showtime s
    SET s.status = :completed
    WHERE s.status IN (:scheduled, :showing)
    AND s.endTime <= :now
""")
    int updateCompletedShowtimes(
            @Param("now") LocalDateTime now,
            @Param("scheduled") ShowtimeStatus scheduled,
            @Param("showing") ShowtimeStatus showing,
            @Param("completed") ShowtimeStatus completed
    );

    @Modifying
    @Query("""
    UPDATE Showtime s
    SET s.status = :showing
    WHERE s.status = :scheduled
    AND s.startTime <= :now
    AND s.endTime > :now
""")
    int updateShowingShowtimes(
            @Param("now") LocalDateTime now,
            @Param("scheduled") ShowtimeStatus scheduled,
            @Param("showing") ShowtimeStatus showing
    );
}