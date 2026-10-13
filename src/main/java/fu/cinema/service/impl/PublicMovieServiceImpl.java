package fu.cinema.service.impl;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.dto.response.ShowtimeResponse;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.repository.MovieRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.TicketRepository;
import fu.cinema.service.PublicMovieService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicMovieServiceImpl implements PublicMovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;

    public PublicMovieServiceImpl(MovieRepository movieRepository, ShowtimeRepository showtimeRepository, TicketRepository ticketRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public List<ShowtimeResponse> getShowtimes(Long movieId, Long branchId, LocalDate date, String timeSlot) {
        List<Showtime> showtimes = showtimeRepository.findAvailableShowtimes(movieId, branchId, LocalDateTime.now());

        return showtimes.stream()
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
                .filter(s -> date == null || s.getStartTime().toLocalDate().equals(date))
                .filter(s -> {
                    if (timeSlot == null || timeSlot.isEmpty()) return true;
                    int hour = s.getStartTime().getHour();
                    return switch (timeSlot) {
                        case "0-3" -> hour >= 0 && hour < 3;
                        case "3-6" -> hour >= 3 && hour < 6;
                        case "6-9" -> hour >= 6 && hour < 9;
                        case "9-12" -> hour >= 9 && hour < 12;
                        case "12-15" -> hour >= 12 && hour < 15;
                        case "15-18" -> hour >= 15 && hour < 18;
                        case "18-21" -> hour >= 18 && hour < 21;
                        case "21-24" -> hour >= 21 && hour <= 23;
                        default -> true;
                    };
                })
                .map(this::mapToShowtimeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> getTop5HighestRevenueMovies() {
        List<Long> top5Ids = ticketRepository.findTop5MoviesByRevenue(PageRequest.of(0, 5));
        return movieRepository.findAllById(top5Ids).stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
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
        int totalSeats = s.getHall().getCapacity();
        int bookedTickets = 0;
        int availableSeats = totalSeats - bookedTickets;

        return ShowtimeResponse.builder()
                .showtimeId(s.getShowtimeId())
                .movieId(s.getMovie().getMovieId())
                .movieTitle(s.getMovie().getTitle())
                .hallId(s.getHall().getHallId())
                .hallName(s.getHall().getHallName())
                // 2. Map thêm thông tin sức chứa và số ghế còn trống
                .hallCapacity(totalSeats)
                .availableSeats(availableSeats)
                .branchId(s.getHall().getBranch().getBranchId())
                .branchName(s.getHall().getBranch().getBranchName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .basePrice(s.getBasePrice())
                .status(s.getStatus())
                .build();
    }
}