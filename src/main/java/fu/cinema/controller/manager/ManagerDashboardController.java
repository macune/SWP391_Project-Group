package fu.cinema.controller.manager;

import fu.cinema.dto.response.RevenueReportResponse;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
import fu.cinema.enums.PaymentStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.PaymentRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/dashboard")
@RequiredArgsConstructor
public class ManagerDashboardController {
    private final ShowtimeRepository showtimeRepository;
    private final StaffRepository staffRepository;
    private final RevenueReportService revenueReportService;

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "week") String period, Model model) {
        if (!List.of("week", "month", "year", "all").contains(period)) period = "week";

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff manager = staffRepository.findByAccount_Username(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin Manager."));
        if (manager.getBranch() == null) throw new IllegalStateException("Manager chưa được gán chi nhánh.");

        Long branchId = manager.getBranch().getBranchId();
        LocalDate today = LocalDate.now();
        LocalDate startDate = switch (period) {
            case "week" -> today.minusDays(6);
            case "month" -> today.minusMonths(1).plusDays(1);
            case "year" -> today.minusYears(1).plusDays(1);
            default -> null;
        };
        LocalDate endDate = today;
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();

        List<Showtime> branchShowtimes = showtimeRepository
                .findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);
        List<Showtime> periodShowtimes = branchShowtimes.stream()
                .filter(s -> from == null || !s.getStartTime().isBefore(from))
                .filter(s -> s.getStartTime().isBefore(until))
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
                .toList();

        long ticketCount = 0;
        long seatCapacity = 0;
        for (Showtime showtime : periodShowtimes) {
            ticketCount += showtime.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();
            seatCapacity += showtime.getHall().getCapacity() == null ? 0L : showtime.getHall().getCapacity();
        }
        double occupancyRate = seatCapacity == 0 ? 0 : ticketCount * 100.0 / seatCapacity;

        RevenueReportResponse report = revenueReportService.getReport(branchId, startDate, endDate, "all");
        BigDecimal totalRevenue = report.totalRevenue() == null ? BigDecimal.ZERO : report.totalRevenue();

        // Aggregate revenue by day for week/month; by month for year/all.
        boolean dailyChart = "week".equals(period) || "month".equals(period);
        Map<String, BigDecimal> revenueByPeriod = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
        if (dailyChart && startDate != null) {
            for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) revenueByPeriod.put(d.format(dayFormatter), BigDecimal.ZERO);
        } else {
            LocalDate firstMonth = startDate == null
                    ? (report.transactions().isEmpty() ? today.withDayOfMonth(1) : report.transactions().stream().map(t -> t.transactionTime().toLocalDate().withDayOfMonth(1)).min(LocalDate::compareTo).orElse(today.withDayOfMonth(1)))
                    : startDate.withDayOfMonth(1);
            for (LocalDate d = firstMonth; !d.isAfter(today); d = d.plusMonths(1)) revenueByPeriod.put(d.format(monthFormatter), BigDecimal.ZERO);
        }
        for (var transaction : report.transactions()) {
            if (transaction.transactionTime() == null) continue;
            LocalDate date = transaction.transactionTime().toLocalDate();
            String key = dailyChart ? date.format(dayFormatter) : date.withDayOfMonth(1).format(monthFormatter);
            if (revenueByPeriod.containsKey(key)) {
                BigDecimal amount = transaction.amount() == null ? BigDecimal.ZERO : transaction.amount();
                revenueByPeriod.merge(key, amount, BigDecimal::add);
            }
        }

        // Top 5 movies ranked by valid tickets sold in the selected period.
        Map<Long, Map<String, Object>> movieStats = new HashMap<>();
        for (Showtime showtime : periodShowtimes) {
            long sold = showtime.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();
            if (sold == 0) continue;
            Long movieId = showtime.getMovie().getMovieId();
            Map<String, Object> stat = movieStats.computeIfAbsent(movieId, id -> {
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
                .sorted(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("tickets")).reversed())
                .limit(5).toList();

        // Upcoming showtimes are independent from the selected dashboard period.
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> upcomingShowtimes = branchShowtimes.stream()
                .filter(s -> !s.getStartTime().isBefore(now))
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
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
                }).toList();

        model.addAttribute("currentBranch", manager.getBranch());
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
        model.addAttribute("chartLabels", new ArrayList<>(revenueByPeriod.keySet()));
        model.addAttribute("chartValues", revenueByPeriod.values().stream().map(BigDecimal::doubleValue).toList());
        model.addAttribute("topMovies", topMovies);
        model.addAttribute("upcomingShowtimes", upcomingShowtimes);
        return "manager/dashboard";
    }
}
