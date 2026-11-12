package fu.cinema.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        String period,
        String periodLabel,
        BigDecimal totalRevenue,
        long totalTickets,
        int totalShowtimes,
        double occupancyRate,
        List<String> chartLabels,
        List<Double> chartValues,
        List<DashboardTopMovieResponse> topMovies,
        List<DashboardUpcomingShowtimeResponse> upcomingShowtimes
) {
}
