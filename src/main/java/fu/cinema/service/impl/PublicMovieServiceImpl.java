package fu.cinema.service.impl;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.dto.response.MovieScheduleResponse;
import fu.cinema.dto.response.ShowtimeResponse;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Seat;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.SeatStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.MovieRepository;
import fu.cinema.repository.SeatRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.TicketRepository;
import fu.cinema.service.PublicMovieService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PublicMovieServiceImpl implements PublicMovieService {
    MovieRepository movieRepository;
    ShowtimeRepository showtimeRepository;
    TicketRepository ticketRepository;
    SeatRepository seatRepository;

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

    // 1. NGHIỆP VỤ LỌC PHIM ĐA TIÊU CHÍ (ĐẨY TỪ CUSTOMER MOVIE CONTROLLER XUỐNG)
    @Override
    public List<MovieResponse> getFilteredMovies(String statusName, String keyword, String genre, String language, String ageRating) {
        List<MovieResponse> allMovies = getMoviesByStatus(statusName);
        return allMovies.stream()
                .filter(m -> keyword == null || keyword.trim().isEmpty() || m.getTitle().toLowerCase().contains(keyword.toLowerCase().trim()))
                .filter(m -> genre == null || genre.isEmpty() || (m.getGenre() != null && m.getGenre().equals(genre)))
                .filter(m -> language == null || language.isEmpty() || (m.getLanguage() != null && m.getLanguage().equals(language)))
                .filter(m -> ageRating == null || ageRating.isEmpty() || (m.getAgeRating() != null && m.getAgeRating().equals(ageRating)))
                .collect(Collectors.toList());
    }

    // 2. NGHIỆP VỤ LẤY LỊCH CHIẾU HÔM NAY CHO STAFF (ĐẨY TỪ STAFF MOVIE CONTROLLER XUỐNG)
    @Override
    public List<MovieScheduleResponse> getTodayMovieSchedules(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<MovieResponse> nowShowingMovies = getMoviesByStatus("NOW_SHOWING");
        List<MovieScheduleResponse> movieSchedules = new ArrayList<>();

        for (MovieResponse movie : nowShowingMovies) {
            List<ShowtimeResponse> allShowtimes = getShowtimes(movie.getMovieId(), branchId, today, null);
            List<ShowtimeResponse> futureShowtimes = allShowtimes.stream()
                    .filter(st -> !st.getStartTime().isBefore(now))
                    .collect(Collectors.toList());

            if (!futureShowtimes.isEmpty()) {
                movieSchedules.add(new MovieScheduleResponse(movie, futureShowtimes));
            }
        }
        return movieSchedules;
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
        List<Seat> allSeats = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(s.getHall().getHallId());
        int activeSeats = (int) allSeats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.ACTIVE)
                .count();

        List<TicketStatus> occupiedStatuses = List.of(
                TicketStatus.BOOKED,
                TicketStatus.LOCKED,
                TicketStatus.CHECKED_IN
        );
        List<Long> bookedSeatIds = ticketRepository.findBookedSeatIdsByShowtimeId(s.getShowtimeId(), occupiedStatuses);
        int bookedTickets = (bookedSeatIds != null) ? bookedSeatIds.size() : 0;
        int availableSeats = Math.max(0, activeSeats - bookedTickets);

        return ShowtimeResponse.builder()
                .showtimeId(s.getShowtimeId())
                .movieId(s.getMovie().getMovieId())
                .movieTitle(s.getMovie().getTitle())
                .hallId(s.getHall().getHallId())
                .hallName(s.getHall().getHallName())
                .hallCapacity(activeSeats)
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