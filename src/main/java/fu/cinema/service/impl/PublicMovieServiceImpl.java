package fu.cinema.service.impl;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.dto.response.ShowtimeResponse;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.MovieStatus;
import fu.cinema.repository.MovieRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.PublicMovieService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicMovieServiceImpl implements PublicMovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public PublicMovieServiceImpl(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public List<MovieResponse> getMoviesByStatus(String statusName) {
        MovieStatus status = MovieStatus.valueOf(statusName);
        return movieRepository.findByStatus(status).stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> searchMovies(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getMoviesByStatus("NOW_SHOWING");
        }
        return movieRepository.findByTitleContainingIgnoreCase(keyword.trim()).stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MovieResponse getMovieDetail(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim!"));
        return mapToMovieResponse(movie);
    }

    @Override
    public List<ShowtimeResponse> getShowtimes(Long movieId, Long branchId) {
        // Lấy các suất chiếu bắt đầu từ thời điểm hiện tại
        return showtimeRepository.findAvailableShowtimes(movieId, branchId, LocalDateTime.now()).stream()
                .map(this::mapToShowtimeResponse)
                .collect(Collectors.toList());
    }

    private MovieResponse mapToMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .releaseDate(movie.getReleaseDate())
                .language(movie.getLanguage())
                .genre(movie.getGenre())
                .ageRating(movie.getAgeRating())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .status(movie.getStatus())
                .build();
    }

    private ShowtimeResponse mapToShowtimeResponse(Showtime s) {
        return ShowtimeResponse.builder()
                .showtimeId(s.getShowtimeId())
                .movieId(s.getMovie().getMovieId())
                .movieTitle(s.getMovie().getTitle())
                .hallId(s.getHall().getHallId())
                .hallName(s.getHall().getHallName())
                .branchId(s.getHall().getBranch().getBranchId())
                .branchName(s.getHall().getBranch().getBranchName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .basePrice(s.getBasePrice())
                .status(s.getStatus())
                .build();
    }
}