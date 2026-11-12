package fu.cinema.service;

import fu.cinema.dto.request.ShowtimeRequest;
import fu.cinema.entity.Hall;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {

    void createShowtime(ShowtimeRequest request, Long branchId);
    void cancelShowtime(Long showtimeId, Long branchId);
    void updateShowtime(
            Long showtimeId,
            ShowtimeRequest request,
            Long branchId
    );
    void updateShowtimeStatuses();

    Showtime getShowtimeForBranch(Long showtimeId, Long branchId);
    boolean isMovieInBranch(Long branchId, Long movieId);

    Page<Showtime> getShowtimesSchedule(
            Long branchId,
            Long hallId,
            ShowtimeStatus status,
            LocalDate showDate,
            Integer month,
            Integer year,
            Pageable pageable
    );

    List<Hall> getHallsByBranch(Long branchId);

    List<Movie> getActiveMovies();

    Showtime getShowtimeForEdit(Long showtimeId, Long branchId);
}
