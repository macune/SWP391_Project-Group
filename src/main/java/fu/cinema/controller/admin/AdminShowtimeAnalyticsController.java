
package fu.cinema.controller.admin;

import fu.cinema.dto.response.BranchResponse;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin/showtime-analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShowtimeAnalyticsController {

    private final ShowtimeRepository showtimeRepository;
    private final BranchService branchService;

    @GetMapping
    public String showAnalytics(
            @RequestParam(required = false) Long branchId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "movie") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        List<BranchResponse> branches = branchService.getAllBranches();

        if (branchId != null && branches.stream()
                .noneMatch(b -> b.getBranchId().equals(branchId))) {
            throw new IllegalArgumentException("Chi nhánh không tồn tại.");
        }

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        if (endDate.isBefore(startDate)) {
            model.addAttribute("dateError",
                    "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
            endDate = startDate;
        }

        if (!List.of("movie", "time", "revenue").contains(sortBy)) {
            sortBy = "movie";
        }
        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "asc";
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        // Lấy suất chiếu của một chi nhánh hoặc toàn bộ chi nhánh
        List<Showtime> allShowtimes;

        if (branchId == null) {
            allShowtimes = showtimeRepository.findAll();
        } else {
            allShowtimes =
                    showtimeRepository
                            .findByHall_Branch_BranchIdOrderByStartTimeAsc(
                                    branchId);
        }

        List<Showtime> showtimes = allShowtimes.stream()
                .filter(s -> s.getHall() != null
                        && s.getHall().getBranch() != null)
                .filter(s -> !s.getStartTime().isBefore(from)
                        && s.getStartTime().isBefore(to))
                .filter(s -> movieId == null
                        || s.getMovie().getMovieId().equals(movieId))
                .filter(s -> status == null || s.getStatus() == status)
                .toList();

        Comparator<Showtime> comparator = switch (sortBy) {
            case "time" ->
                    Comparator.comparing(Showtime::getStartTime);
            case "revenue" ->
                    Comparator.comparing(this::getShowtimeRevenue);
            default ->
                    Comparator.comparing(
                            s -> s.getMovie().getTitle(),
                            String.CASE_INSENSITIVE_ORDER);
        };

        if ("desc".equals(sortDir)) {
            comparator = comparator.reversed();
        }

        showtimes = showtimes.stream()
                .sorted(comparator)
                .toList();

        long totalTickets = 0;
        long totalSeats = 0;
        BigDecimal ticketRevenue = BigDecimal.ZERO;

        List<Map<String, Object>> showtimeRows = new ArrayList<>();

        Map<String, Long> ticketsByHour = new LinkedHashMap<>();
        List<String> hourLabels = List.of(
                "06–08h", "08–10h", "10–12h", "12–14h",
                "14–16h", "16–18h", "18–20h", "20–22h", "22–24h"
        );
        hourLabels.forEach(label -> ticketsByHour.put(label, 0L));

        Map<DayOfWeek, Long> ticketsByDay = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            ticketsByDay.put(day, 0L);
        }

        for (Showtime s : showtimes) {
            long soldTickets = s.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED
                            || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();

            BigDecimal revenue = getShowtimeRevenue(s);

            long capacity = s.getHall().getCapacity() == null
                    ? 0L : s.getHall().getCapacity();

            double occupancy = capacity == 0
                    ? 0.0 : soldTickets * 100.0 / capacity;

            totalTickets += soldTickets;
            totalSeats += capacity;
            ticketRevenue = ticketRevenue.add(revenue);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("movieTitle", s.getMovie().getTitle());
            row.put("hallName", s.getHall().getHallName());
            row.put("branchName", s.getHall().getBranch().getBranchName());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("status", s.getStatus());
            row.put("soldTickets", soldTickets);
            row.put("capacity", capacity);
            row.put("occupancy", occupancy);
            row.put("revenue", revenue);

            showtimeRows.add(row);

            int hour = s.getStartTime().getHour();
            int bucket = hour / 2;

            if (bucket >= 3 && bucket <= 11) {
                String label = String.format(
                        "%02d–%02dh", bucket * 2, bucket * 2 + 2);
                if (ticketsByHour.containsKey(label)) {
                    ticketsByHour.merge(label, soldTickets, Long::sum);
                }
            }

            ticketsByDay.merge(
                    s.getStartTime().getDayOfWeek(),
                    soldTickets,
                    Long::sum);
        }

        double occupancyRate = totalSeats == 0
                ? 0.0 : totalTickets * 100.0 / totalSeats;

        List<String> dayLabels = List.of(
                "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5",
                "Thứ 6", "Thứ 7", "Chủ nhật"
        );

        List<Long> dayValues = List.of(
                ticketsByDay.get(DayOfWeek.MONDAY),
                ticketsByDay.get(DayOfWeek.TUESDAY),
                ticketsByDay.get(DayOfWeek.WEDNESDAY),
                ticketsByDay.get(DayOfWeek.THURSDAY),
                ticketsByDay.get(DayOfWeek.FRIDAY),
                ticketsByDay.get(DayOfWeek.SATURDAY),
                ticketsByDay.get(DayOfWeek.SUNDAY)
        );

        // Phân trang 10 suất chiếu/trang, đánh số trang từ 1
        int pageSize = 10;
        int totalElements = showtimeRows.size();
        int totalPages = (int) Math.ceil(
                totalElements / (double) pageSize);

        page = Math.max(1, page);
        page = totalPages > 0 ? Math.min(page, totalPages) : 1;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalElements);

        List<Map<String, Object>> pagedRows =
                showtimeRows.subList(start, end);

        // Danh sách phim để lọc, dựa trên toàn bộ suất chiếu
        List<Showtime> branchShowtimes = allShowtimes.stream()
                .filter(s -> s.getHall() != null
                        && s.getHall().getBranch() != null)
                .toList();

        Map<Long, Object> uniqueMovies = new LinkedHashMap<>();
        branchShowtimes.forEach(s ->
                uniqueMovies.putIfAbsent(
                        s.getMovie().getMovieId(), s.getMovie()));

        model.addAttribute("branches", branches);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("branchLabel", branchId == null
                ? "Toàn bộ chi nhánh"
                : branches.stream()
                .filter(b -> b.getBranchId().equals(branchId))
                .map(BranchResponse::getBranchName)
                .findFirst().orElse("Chi nhánh"));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses",
                Arrays.asList(ShowtimeStatus.values()));
        model.addAttribute("movies", uniqueMovies.values());

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        model.addAttribute("totalShowtimes", showtimes.size());
        model.addAttribute("totalTickets", totalTickets);
        model.addAttribute("totalSeats", totalSeats);
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("ticketRevenue", ticketRevenue);

        model.addAttribute("hourLabels", hourLabels);
        model.addAttribute("hourValues",
                new ArrayList<>(ticketsByHour.values()));
        model.addAttribute("dayLabels", dayLabels);
        model.addAttribute("dayValues", dayValues);

        model.addAttribute("showtimeRows", pagedRows);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", pageSize);

        return "admin/showtime-analytics";
    }

    private BigDecimal getShowtimeRevenue(Showtime showtime) {
        return showtime.getTickets().stream()
                .filter(t -> t.getStatus() == TicketStatus.BOOKED
                        || t.getStatus() == TicketStatus.CHECKED_IN)
                .map(t -> t.getPrice() == null
                        ? BigDecimal.ZERO : t.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
