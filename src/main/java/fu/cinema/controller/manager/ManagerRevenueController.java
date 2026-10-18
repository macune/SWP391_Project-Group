
package fu.cinema.controller.manager;

import fu.cinema.dto.response.FnbRevenueRowResponse;
import fu.cinema.dto.response.RevenueMovieResponse;
import fu.cinema.dto.response.RevenueTransactionResponse;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
import fu.cinema.enums.FnBCategory;
import fu.cinema.enums.MovieStatus;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/manager/revenue")
@RequiredArgsConstructor
public class ManagerRevenueController {

    private final RevenueReportService revenueReportService;
    private final StaffRepository staffRepository;
    private final ShowtimeRepository showtimeRepository;
    @GetMapping("/revenue/detail")
    public String showRevenueReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(defaultValue = "all")
            String revenueType,

            Model model
    ) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Staff staff = staffRepository.findByAccount_Username(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy thông tin Manager."
                        ));

        if (staff.getBranch() == null) {
            throw new IllegalStateException(
                    "Manager chưa được gán chi nhánh."
            );
        }

        if (!revenueType.equals("all")
                && !revenueType.equals("tickets")
                && !revenueType.equals("fnb")) {
            revenueType = "all";
        }

        model.addAttribute("currentBranch", staff.getBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("revenueType", revenueType);
        model.addAttribute(
                "report",
                revenueReportService.getReport(
                        staff.getBranch().getBranchId(),
                        startDate,
                        endDate,
                        revenueType
                )
        );



        return "manager/revenue-report";
    }

    @GetMapping
    public String showMovieRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false) String movieName,
            @RequestParam(required = false) MovieStatus movieStatus,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        if (!List.of("title", "revenue", "showtimes").contains(sortBy)) {
            sortBy = "title";
        }
        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "asc";
        }

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("movieStatus", movieStatus);
        model.addAttribute("movieStatuses", MovieStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        List<RevenueMovieResponse> movies =
                revenueReportService.getMovieReport(
                        branchId, startDate, endDate,
                        movieStatus, sortBy, sortDir
                );

        BigDecimal totalRevenue = movies.stream()
                .map(RevenueMovieResponse::revenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalShowtimes = movies.stream()
                .mapToLong(RevenueMovieResponse::showtimeCount)
                .sum();

        long totalTickets = movies.stream()
                .mapToLong(RevenueMovieResponse::ticketCount)
                .sum();

        long totalSeats = movies.stream()
                .mapToLong(RevenueMovieResponse::totalSeats)
                .sum();

        model.addAttribute("movies", movies);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalShowtimes", totalShowtimes);
        model.addAttribute("totalTickets", totalTickets);
        model.addAttribute("totalSeats", totalSeats);

        if (movieName != null && !movieName.isBlank()) {
            String keyword = movieName.trim().toLowerCase();

            movies = movies.stream()
                    .filter(movie -> movie.movieTitle() != null
                            && movie.movieTitle().toLowerCase().contains(keyword))
                    .toList();
        }

        model.addAttribute("movieName", movieName);
        model.addAttribute("movies", movies);

        return "manager/revenue-movies";
    }
    @GetMapping("/movie/{movieId}")
    public String showMovieShowtimes(
            @PathVariable Long movieId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        // Chỉ cho phép hai kiểu sắp xếp hợp lệ
        if (!"time".equalsIgnoreCase(sortBy)
                && !"revenue".equalsIgnoreCase(sortBy)) {
            sortBy = "time";
        }
        if (!List.of("time", "revenue").contains(sortBy)) {
            sortBy = "time";
        }

        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "desc";
        }

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        boolean movieBelongsToBranch = showtimeRepository
                .findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId)
                .stream()
                .anyMatch(s -> s.getMovie().getMovieId().equals(movieId));

        if (!movieBelongsToBranch) {
            return "redirect:/manager/revenue";
        }

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("movieId", movieId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortBy", sortBy);

        model.addAttribute(
                "showtimes",
                revenueReportService.getShowtimeReport(
                        branchId, movieId, startDate, endDate, sortBy, sortDir
                )
        );

        return "manager/revenue-showtimes";
    }
    @GetMapping("/showtime/{showtimeId}")
    public String showShowtimeDetail(
            @PathVariable Long showtimeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(defaultValue = "all") String paymentMethod,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy suất chiếu."));

        if (showtime.getHall().getBranch() == null
                || !showtime.getHall().getBranch().getBranchId()
                .equals(branchId)) {
            throw new IllegalArgumentException(
                    "Bạn không có quyền xem báo cáo của suất chiếu này."
            );
        }

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("showtime", showtime);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentMethod", paymentMethod);
        var report = revenueReportService.getShowtimeDetail(
                branchId,
                showtimeId,
                startDate,
                endDate,
                paymentMethod
        );

        model.addAttribute("report", report);

        long totalTicketCount = report.transactions()
                .stream()
                .mapToLong(RevenueTransactionResponse::ticketCount)
                .sum();

        model.addAttribute("totalTicketCount", totalTicketCount);

        return "manager/revenue-report";
    }
    @GetMapping("/fnb")
    public String showFnbRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        if (!List.of("ALL", "FOOD", "DRINK", "COMBO").contains(category)) {
            category = "ALL";
        }

        if (!List.of("name", "sold", "revenue").contains(sortBy)) {
            sortBy = "name";
        }

        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "asc";
        }

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("category", category);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        model.addAttribute("items", revenueReportService.getFnbProductRevenue(
                branchId, startDate, endDate, category, sortBy, sortDir));

        return "manager/revenue-fnb";
    }

    @GetMapping("/fnb/{itemId}")
    public String showFnbItemTransactions(
            @PathVariable Long itemId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(defaultValue = "ALL") String paymentMethod,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        List<FnbRevenueRowResponse> transactions =
                revenueReportService.getFnbItemTransactions(
                        branchId, itemId, startDate, endDate,
                        paymentMethod, sortDir);

        int pageSize = 10;
        int totalPages = Math.max(
                1, (int) Math.ceil(transactions.size() / (double) pageSize));

        page = Math.max(0, Math.min(page, totalPages - 1));

        int from = page * pageSize;
        int to = Math.min(from + pageSize, transactions.size());

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("itemId", itemId);
        model.addAttribute("rows", transactions.subList(from, to));
        model.addAttribute("totalRows", transactions.size());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("sortDir", sortDir);
        return "manager/revenue-fnb-detail";
    }
    private Staff getCurrentManager() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Staff staff = staffRepository.findByAccount_Username(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy thông tin Manager."
                        ));

        if (staff.getBranch() == null) {
            throw new IllegalStateException(
                    "Manager chưa được gán chi nhánh."
            );
        }

        return staff;
    }

    private Long getCurrentManagerBranchId() {
        return getCurrentManager().getBranch().getBranchId();
    }

    private fu.cinema.entity.Branch getCurrentManagerBranch() {
        return getCurrentManager().getBranch();
    }
}
