package fu.cinema.service.impl;

import fu.cinema.dto.response.DashboardResponse;
import fu.cinema.dto.response.DashboardTopMovieResponse;
import fu.cinema.dto.response.DashboardUpcomingShowtimeResponse;
import fu.cinema.dto.response.RevenueReportResponse;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.DashboardService;
import fu.cinema.service.RevenueReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardServiceImpl implements DashboardService {

    ShowtimeRepository showtimeRepository;
    RevenueReportService revenueReportService;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Long branchId, String period) {
        if (!List.of("week", "month", "year", "all").contains(period)) {
            period = "week";
        }

        LocalDate today = LocalDate.now();

        LocalDate startDate = switch (period) {
            case "week" -> today.minusDays(6);
            case "month" -> today.minusMonths(1).plusDays(1);
            case "year" -> today.minusYears(1).plusDays(1);
            default -> null;
        };

        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();

        // Lấy suất chiếu theo chi nhánh hoặc toàn chuỗi
        List<Showtime> allShowtimes = branchId == null
                ? showtimeRepository.findAll()
                : showtimeRepository.findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);

        List<Showtime> periodShowtimes = allShowtimes.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> from == null || !s.getStartTime().isBefore(from))
                .filter(s -> s.getStartTime().isBefore(until))
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
                .toList();

        long ticketCount = 0;
        long seatCapacity = 0;

        for (Showtime showtime : periodShowtimes) {
            if (showtime.getTickets() != null) {
                ticketCount += showtime.getTickets().stream()
                        .filter(t -> t.getStatus() == TicketStatus.BOOKED
                                || t.getStatus() == TicketStatus.CHECKED_IN)
                        .count();
            }

            if (showtime.getHall() != null && showtime.getHall().getCapacity() != null) {
                seatCapacity += showtime.getHall().getCapacity();
            }
        }

        double occupancyRate = seatCapacity == 0 ? 0 : ticketCount * 100.0 / seatCapacity;

        // Doanh thu
        RevenueReportResponse report = revenueReportService.getReport(branchId, startDate, today, "all");
        BigDecimal totalRevenue = report.totalRevenue() == null ? BigDecimal.ZERO : report.totalRevenue();

        boolean dailyChart = "week".equals(period) || "month".equals(period);
        Map<String, BigDecimal> revenueByPeriod = new LinkedHashMap<>();

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        if (dailyChart && startDate != null) {
            for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
                revenueByPeriod.put(d.format(dayFormatter), BigDecimal.ZERO);
            }
        } else {
            LocalDate firstMonth = startDate == null
                    ? (report.transactions() == null || report.transactions().isEmpty()
                    ? today.withDayOfMonth(1)
                    : report.transactions().stream()
                    .filter(t -> t.transactionTime() != null)
                    .map(t -> t.transactionTime().toLocalDate().withDayOfMonth(1))
                    .min(LocalDate::compareTo)
                    .orElse(today.withDayOfMonth(1)))
                    : startDate.withDayOfMonth(1);

            for (LocalDate d = firstMonth; !d.isAfter(today); d = d.plusMonths(1)) {
                revenueByPeriod.put(d.format(monthFormatter), BigDecimal.ZERO);
            }
        }

        if (report.transactions() != null) {
            for (var transaction : report.transactions()) {
                if (transaction.transactionTime() == null) continue;

                LocalDate date = transaction.transactionTime().toLocalDate();
                String key = dailyChart
                        ? date.format(dayFormatter)
                        : date.withDayOfMonth(1).format(monthFormatter);

                if (revenueByPeriod.containsKey(key)) {
                    BigDecimal amount = transaction.amount() == null ? BigDecimal.ZERO : transaction.amount();
                    revenueByPeriod.merge(key, amount, BigDecimal::add);
                }
            }
        }

        // Top 5 phim theo số vé hợp lệ
        Map<Long, DashboardTopMovieResponse> movieStats = new HashMap<>();

        for (Showtime showtime : periodShowtimes) {
            if (showtime.getTickets() == null || showtime.getMovie() == null) continue;

            long sold = showtime.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED
                            || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();

            if (sold == 0) continue;

            Long movieId = showtime.getMovie().getMovieId();

            movieStats.compute(movieId, (id, existing) -> {
                long currentTickets = (existing == null) ? 0L : existing.tickets();
                return new DashboardTopMovieResponse(
                        id,
                        showtime.getMovie().getTitle(),
                        showtime.getMovie().getPosterUrl(),
                        currentTickets + sold
                );
            });
        }

        List<DashboardTopMovieResponse> topMovies = movieStats.values().stream()
                .sorted(Comparator.comparingLong(DashboardTopMovieResponse::tickets).reversed())
                .limit(5)
                .toList();

        // 5 suất chiếu sắp tới
        LocalDateTime now = LocalDateTime.now();

        List<DashboardUpcomingShowtimeResponse> upcomingShowtimes = allShowtimes.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> !s.getStartTime().isBefore(now))
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .limit(5)
                .map(s -> new DashboardUpcomingShowtimeResponse(
                        s.getMovie() != null ? s.getMovie().getTitle() : null,
                        s.getMovie() != null ? s.getMovie().getPosterUrl() : null,
                        s.getHall() != null ? s.getHall().getHallName() : null,
                        s.getStartTime(),
                        s.getStatus()
                ))
                .toList();

        String periodLabel = switch (period) {
            case "week" -> "7 ngày gần nhất";
            case "month" -> "1 tháng gần nhất";
            case "year" -> "1 năm gần nhất";
            default -> "Toàn bộ dữ liệu";
        };

        return new DashboardResponse(
                period,
                periodLabel,
                totalRevenue,
                ticketCount,
                periodShowtimes.size(),
                occupancyRate,
                new ArrayList<>(revenueByPeriod.keySet()),
                revenueByPeriod.values().stream().map(BigDecimal::doubleValue).toList(),
                topMovies,
                upcomingShowtimes
        );
    }
}
