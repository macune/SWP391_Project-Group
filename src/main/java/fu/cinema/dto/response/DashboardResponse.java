package fu.cinema.dto.response;

import fu.cinema.entity.Showtime;
import fu.cinema.entity.Ticket;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    public record Staff(
            String branchName,
            LocalDate today,
            BigDecimal dailyRevenue,
            BigDecimal ticketRevenue,
            BigDecimal fnbRevenue,
            int todayShowtimesCount,
            long availableFnbItemsCount,
            List<Showtime> upcomingShowtimes
    ) {}

    public record Customer(
            long upcomingTicketCount,
            long completedBookingCount,
            Page<Ticket> ticketPage,
            List<TopMovie> topMovies,
            LocalDate showDate,
            Integer month,
            Integer year,
            LocalDate bookingDate,
            Integer bookingMonth,
            Integer bookingYear
    ) {
        public record TopMovie(
                MovieResponse movie,
                long ticketCount
        ) {}
    }
}
