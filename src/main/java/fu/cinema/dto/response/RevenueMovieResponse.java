package fu.cinema.dto.response;

import fu.cinema.enums.MovieStatus;

import java.math.BigDecimal;

public record RevenueMovieResponse(
        Long movieId,
        String movieTitle,
        String posterUrl,
        MovieStatus status,
        long showtimeCount,
        long ticketCount,
        long totalSeats,
        BigDecimal revenue
) {
}
