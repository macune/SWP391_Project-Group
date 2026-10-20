
package fu.cinema.controller.manager;

import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/manager/showtime-analytics")
@RequiredArgsConstructor
public class ManagerShowtimeAnalyticsController {

    private final ShowtimeRepository showtimeRepository;
    private final StaffRepository staffRepository;

    @GetMapping
    public String showAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false, defaultValue = "movie") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Staff manager = staffRepository.findByAccount_Username(username)
                .orElseThrow(() ->
                        new IllegalStateException("Không tìm thấy Manager."));

        if (manager.getBranch() == null) {
            throw new IllegalStateException(
                    "Manager chưa được gán chi nhánh.");
        }

        Long branchId = manager.getBranch().getBranchId();

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

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        List<Showtime> allShowtimes =
                showtimeRepository
                        .findByHall_Branch_BranchIdOrderByStartTimeAsc(
                                branchId);

        List<Showtime> showtimes = allShowtimes.stream()
                .filter(s -> !s.getStartTime().isBefore(from)
                        && s.getStartTime().isBefore(to))
                .filter(s -> movieId == null
                        || s.getMovie().getMovieId().equals(movieId))
                .filter(s -> status == null || s.getStatus() == status)
                .toList();
        if (!List.of("movie", "time", "revenue").contains(sortBy)) {
            sortBy = "movie";
        }

        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "asc";
        }
        Comparator<Showtime> comparator;

        switch (sortBy) {
            case "time" ->
                    comparator = Comparator.comparing(Showtime::getStartTime);

            case "revenue" ->
                    comparator = Comparator.comparing(s ->
                            s.getTickets().stream()
                                    .filter(t -> t.getStatus() == TicketStatus.BOOKED
                                            || t.getStatus() == TicketStatus.CHECKED_IN)
                                    .map(t -> t.getPrice() == null
                                            ? BigDecimal.ZERO : t.getPrice())
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    );

            default ->
                    comparator = Comparator.comparing(
                            s -> s.getMovie().getTitle(),
                            String.CASE_INSENSITIVE_ORDER
                    );
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        showtimes = showtimes.stream()
                .sorted(comparator)
                .toList();

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        List<Map<String, Object>> showtimeRows = new ArrayList<>();

        long totalTickets = 0;
        long totalSeats = 0;
        BigDecimal ticketRevenue = BigDecimal.ZERO;

        for (Showtime s : showtimes) {
            long soldTickets = 0;
            BigDecimal revenue = BigDecimal.ZERO;

            for (var ticket : s.getTickets()) {
                if (ticket.getStatus() == TicketStatus.BOOKED
                        || ticket.getStatus() == TicketStatus.CHECKED_IN) {
                    soldTickets++;
                    if (ticket.getPrice() != null) {
                        revenue = revenue.add(ticket.getPrice());
                    }
                }
            }

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
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("status", s.getStatus());
            row.put("soldTickets", soldTickets);
            row.put("capacity", capacity);
            row.put("occupancy", occupancy);
            row.put("revenue", revenue);

            showtimeRows.add(row);
        }

        long totalShowtimes = showtimes.size();
        double occupancyRate = totalSeats == 0
                ? 0.0 : totalTickets * 100.0 / totalSeats;

// Biểu đồ lượng vé theo khung giờ, mỗi khung 2 tiếng
        List<String> hourLabels = List.of(
                "06–08h", "08–10h", "10–12h", "12–14h",
                "14–16h", "16–18h", "18–20h", "20–22h", "22–24h"
        );

        Map<String, Long> ticketsByHour = new LinkedHashMap<>();
        hourLabels.forEach(label -> ticketsByHour.put(label, 0L));

        Map<DayOfWeek, Long> ticketsByDay = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            ticketsByDay.put(day, 0L);
        }

        for (Showtime s : showtimes) {
            long sold = s.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED
                            || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();

            int hour = s.getStartTime().getHour();
            int bucket = hour / 2;

            if (bucket >= 3 && bucket <= 11) {
                String label = String.format("%02d–%02dh",
                        bucket * 2, bucket * 2 + 2);

                if (ticketsByHour.containsKey(label)) {
                    ticketsByHour.merge(label, sold, Long::sum);
                }
            }

            DayOfWeek day = s.getStartTime().getDayOfWeek();
            ticketsByDay.merge(day, sold, Long::sum);
        }

        List<String> dayLabels = List.of(
                "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5",
                "Thứ 6", "Thứ 7", "Chủ nhật"
        );

        List<Long> dayValues = new ArrayList<>();
        for (DayOfWeek day : List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY)) {
            dayValues.add(ticketsByDay.get(day));
        }

        model.addAttribute("hourLabels",
                new ArrayList<>(ticketsByHour.keySet()));
        model.addAttribute("hourValues",
                new ArrayList<>(ticketsByHour.values()));
        model.addAttribute("dayLabels", dayLabels);
        model.addAttribute("dayValues", dayValues);

        model.addAttribute("showtimeRows", showtimeRows);
        model.addAttribute("totalShowtimes", totalShowtimes);
        model.addAttribute("totalTickets", totalTickets);
        model.addAttribute("totalSeats", totalSeats);
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("ticketRevenue", ticketRevenue);


        model.addAttribute("currentBranch", manager.getBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", Arrays.asList(ShowtimeStatus.values()));

        model.addAttribute("movies", allShowtimes.stream()
                .map(Showtime::getMovie)
                .distinct()
                .toList());
        // Pagination: 10 showtimes per page
        size = 10;

        int totalElements = showtimes.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        page = Math.max(1, page);
        page = totalPages > 0 ? Math.min(page, totalPages) : 1;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalElements);

// Chỉ lấy 10 suất chiếu của trang hiện tại
        List<Showtime> pagedShowtimes = showtimes.subList(start, end);

// Lấy các dòng dữ liệu tương ứng với 10 suất chiếu
        List<Map<String, Object>> pagedRows = showtimeRows.subList(start, end);

        model.addAttribute("showtimes", pagedShowtimes);
        model.addAttribute("showtimeRows", pagedRows);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", size);

        return "manager/showtime-analytics";
    }
}
