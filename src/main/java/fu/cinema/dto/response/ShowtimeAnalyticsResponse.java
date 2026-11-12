package fu.cinema.dto.response;

import fu.cinema.entity.Movie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ShowtimeAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        String dateError,
        int totalShowtimes,
        long totalTickets,
        long totalSeats,
        double occupancyRate,
        BigDecimal ticketRevenue,
        List<String> hourLabels,
        List<Long> hourValues,
        List<String> dayLabels,
        List<Long> dayValues,
        List<Movie> movies,
        List<ShowtimeAnalyticsRowResponse> showtimeRows,
        int currentPage,
        int totalPages,
        int totalElements,
        int pageSize
) {
}
