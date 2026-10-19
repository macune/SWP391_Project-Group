
package fu.cinema.controller.admin;

import fu.cinema.dto.response.RevenueReportResponse;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.PaymentStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.PaymentRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.RevenueReportService;
import fu.cinema.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final ShowtimeRepository showtimeRepository;
    private final PaymentRepository paymentRepository;
    private final RevenueReportService revenueReportService;
    private final BranchService branchService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "week") String period,
            Model model) {

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

        LocalDateTime from = startDate == null
                ? null : startDate.atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();

        // Lấy suất chiếu theo chi nhánh hoặc toàn chuỗi.
        List<Showtime> allShowtimes = branchId == null
                ? showtimeRepository.findAll()
                : showtimeRepository
                .findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);

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

            if (showtime.getHall() != null
                    && showtime.getHall().getCapacity() != null) {
                seatCapacity += showtime.getHall().getCapacity();
            }
        }

        double occupancyRate = seatCapacity == 0
                ? 0 : ticketCount * 100.0 / seatCapacity;

        // Doanh thu: cần repository hỗ trợ branchId null.
        RevenueReportResponse report =
                revenueReportService.getReport(
                        branchId, startDate, today, "all");

        BigDecimal totalRevenue = report.totalRevenue() == null
                ? BigDecimal.ZERO : report.totalRevenue();

        boolean dailyChart = "week".equals(period)
                || "month".equals(period);

        Map<String, BigDecimal> revenueByPeriod =
                new LinkedHashMap<>();

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter monthFormatter =
                DateTimeFormatter.ofPattern("MM/yyyy");

        if (dailyChart && startDate != null) {
            for (LocalDate d = startDate;
                 !d.isAfter(today); d = d.plusDays(1)) {
                revenueByPeriod.put(
                        d.format(dayFormatter), BigDecimal.ZERO);
            }
        } else {
            LocalDate firstMonth = startDate == null
                    ? (report.transactions().isEmpty()
                    ? today.withDayOfMonth(1)
                    : report.transactions().stream()
                    .filter(t -> t.transactionTime() != null)
                    .map(t -> t.transactionTime()
                            .toLocalDate().withDayOfMonth(1))
                    .min(LocalDate::compareTo)
                    .orElse(today.withDayOfMonth(1)))
                    : startDate.withDayOfMonth(1);

            for (LocalDate d = firstMonth;
                 !d.isAfter(today); d = d.plusMonths(1)) {
                revenueByPeriod.put(
                        d.format(monthFormatter), BigDecimal.ZERO);
            }
        }

        for (var transaction : report.transactions()) {
            if (transaction.transactionTime() == null) continue;

            LocalDate date = transaction.transactionTime().toLocalDate();
            String key = dailyChart
                    ? date.format(dayFormatter)
                    : date.withDayOfMonth(1).format(monthFormatter);

            if (revenueByPeriod.containsKey(key)) {
                BigDecimal amount = transaction.amount() == null
                        ? BigDecimal.ZERO : transaction.amount();
                revenueByPeriod.merge(key, amount, BigDecimal::add);
            }
        }

        // Top 5 phim theo số vé hợp lệ.
        Map<Long, Map<String, Object>> movieStats = new HashMap<>();

        for (Showtime showtime : periodShowtimes) {
            if (showtime.getTickets() == null
                    || showtime.getMovie() == null) continue;

            long sold = showtime.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED
                            || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();

            if (sold == 0) continue;

            Long movieId = showtime.getMovie().getMovieId();

            Map<String, Object> stat = movieStats.computeIfAbsent(
                    movieId, id -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("movieId", id);
                        m.put("title", showtime.getMovie().getTitle());
                        m.put("posterUrl", showtime.getMovie().getPosterUrl());
                        m.put("tickets", 0L);
                        return m;
                    });

            stat.put("tickets", (Long) stat.get("tickets") + sold);
        }

        List<Map<String, Object>> topMovies = movieStats.values().stream()
                .sorted(Comparator.comparingLong(
                        (Map<String, Object> m) ->
                                (Long) m.get("tickets")).reversed())
                .limit(5)
                .toList();

        // 5 suất chiếu sắp tới.
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> upcomingShowtimes =
                allShowtimes.stream()
                        .filter(s -> s.getStartTime() != null)
                        .filter(s -> !s.getStartTime().isBefore(now))
                        .filter(s -> s.getStatus()
                                != ShowtimeStatus.CANCELLED)
                        .sorted(Comparator.comparing(Showtime::getStartTime))
                        .limit(5)
                        .map(s -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("movieTitle", s.getMovie().getTitle());
                            row.put("posterUrl", s.getMovie().getPosterUrl());
                            row.put("hallName", s.getHall().getHallName());
                            row.put("startTime", s.getStartTime());
                            row.put("status", s.getStatus());
                            return row;
                        })
                        .toList();

        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("period", period);
        model.addAttribute("periodLabel", switch (period) {
            case "week" -> "7 ngày gần nhất";
            case "month" -> "1 tháng gần nhất";
            case "year" -> "1 năm gần nhất";
            default -> "Toàn bộ dữ liệu";
        });

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalTickets", ticketCount);
        model.addAttribute("totalShowtimes", periodShowtimes.size());
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("chartLabels",
                new ArrayList<>(revenueByPeriod.keySet()));
        model.addAttribute("chartValues",
                revenueByPeriod.values().stream()
                        .map(BigDecimal::doubleValue).toList());
        model.addAttribute("topMovies", topMovies);
        model.addAttribute("upcomingShowtimes", upcomingShowtimes);

        return "admin/dashboard";
    }
}
