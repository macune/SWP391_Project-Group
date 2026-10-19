package fu.cinema.controller.admin;

import fu.cinema.dto.response.*;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.MovieStatus;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.BranchService;
import fu.cinema.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRevenueController {

    private final RevenueReportService revenueReportService;
    private final BranchService branchService;
    private final ShowtimeRepository showtimeRepository;

    @GetMapping
    public String showMovieRevenue(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String movieName,
            @RequestParam(required = false) MovieStatus movieStatus,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        validateBranchId(branchId);
        sortBy = List.of("title", "revenue", "showtimes").contains(sortBy) ? sortBy : "title";
        sortDir = List.of("asc", "desc").contains(sortDir) ? sortDir : "asc";

        List<RevenueMovieResponse> movies = getMovieRows(branchId, startDate, endDate, movieStatus, sortBy, sortDir);
        if (movieName != null && !movieName.isBlank()) {
            String keyword = movieName.trim().toLowerCase(Locale.ROOT);
            movies = movies.stream().filter(m -> m.movieTitle() != null
                    && m.movieTitle().toLowerCase(Locale.ROOT).contains(keyword)).toList();
        }
        int pageSize = 10;
        int totalRows = movies.size();
        int totalPages = Math.max(
                1, (int) Math.ceil(totalRows / (double) pageSize)
        );

        page = Math.max(0, Math.min(page, totalPages - 1));

        int from = page * pageSize;
        int to = Math.min(from + pageSize, totalRows);

        List<RevenueMovieResponse> pageItems =
                movies.subList(from, to);

        model.addAttribute("items", pageItems);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalRows", totalRows);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", findBranch(branchId));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("movieName", movieName);
        model.addAttribute("movieStatus", movieStatus);
        model.addAttribute("movieStatuses", MovieStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("movies", movies);
        model.addAttribute("totalRevenue", movies.stream().map(RevenueMovieResponse::revenue)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("totalShowtimes", movies.stream().mapToLong(RevenueMovieResponse::showtimeCount).sum());
        model.addAttribute("totalTickets", movies.stream().mapToLong(RevenueMovieResponse::ticketCount).sum());
        model.addAttribute("totalSeats", movies.stream().mapToLong(RevenueMovieResponse::totalSeats).sum());
        return "admin/revenue-movies";
    }

    @GetMapping("/movie/{movieId}")
    public String showMovieShowtimes(
            @PathVariable Long movieId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        validateBranchId(branchId);
        sortBy = List.of("time", "revenue").contains(sortBy) ? sortBy : "time";
        sortDir = List.of("asc", "desc").contains(sortDir) ? sortDir : "desc";
        List<RevenueShowtimeResponse> showtimes = new ArrayList<>();
        Map<Long, String> showtimeBranchNames = new HashMap<>();
        for (BranchResponse branch : branchesFor(branchId)) {
            List<RevenueShowtimeResponse> branchShowtimes = revenueReportService.getShowtimeReport(
                    branch.getBranchId(), movieId, startDate, endDate, sortBy, sortDir);
            showtimes.addAll(branchShowtimes);
            for (RevenueShowtimeResponse row : branchShowtimes) {
                showtimeBranchNames.put(row.showtimeId(), branch.getBranchName());
            }
        }
        if ("revenue".equals(sortBy)) {
            showtimes.sort(Comparator.comparing(RevenueShowtimeResponse::revenue,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            showtimes.sort(Comparator.comparing(RevenueShowtimeResponse::startTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        if ("desc".equals(sortDir)) Collections.reverse(showtimes);
        int pageSize = 10;
        int totalRows = showtimes.size();
        int totalPages = Math.max(
                1, (int) Math.ceil(totalRows / (double) pageSize)
        );

        page = Math.max(0, Math.min(page, totalPages - 1));

        int from = page * pageSize;
        int to = Math.min(from + pageSize, totalRows);

        List<RevenueShowtimeResponse> pageItems =
                showtimes.subList(from, to);

        model.addAttribute("items", pageItems);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalRows", totalRows);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", findBranch(branchId));
        model.addAttribute("movieId", movieId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("showtimeBranchNames", showtimeBranchNames);
        return "admin/revenue-showtimes";
    }

    @GetMapping("/showtime/{showtimeId}")
    public String showShowtimeDetail(
            @PathVariable Long showtimeId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "all") String paymentMethod,
            Model model) {

        validateBranchId(branchId);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
        if (showtime.getHall() == null || showtime.getHall().getBranch() == null) {
            throw new IllegalArgumentException("Suất chiếu chưa được gán chi nhánh.");
        }
        Long actualBranchId = showtime.getHall().getBranch().getBranchId();
        if (branchId != null && !branchId.equals(actualBranchId)) {
            throw new IllegalArgumentException("Suất chiếu không thuộc chi nhánh đã chọn.");
        }

        RevenueReportResponse report = revenueReportService.getShowtimeDetail(
                actualBranchId, showtimeId, startDate, endDate, paymentMethod);
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", findBranch(actualBranchId));
        model.addAttribute("showtime", showtime);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("report", report);
        model.addAttribute("totalTicketCount", report.transactions().stream()
                .mapToLong(RevenueTransactionResponse::ticketCount).sum());
        return "admin/revenue-report";
    }

    @GetMapping("/fnb")
    public String showFnbRevenue(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        validateBranchId(branchId);
        category = List.of("ALL", "FOOD", "DRINK", "COMBO").contains(category) ? category : "ALL";
        sortBy = List.of("name", "sold", "revenue").contains(sortBy) ? sortBy : "name";
        sortDir = List.of("asc", "desc").contains(sortDir) ? sortDir : "asc";

        Map<Long, FnbProductRevenueResponse> grouped = new LinkedHashMap<>();
        for (BranchResponse branch : branchesFor(branchId)) {
            for (FnbProductRevenueResponse item : revenueReportService.getFnbProductRevenue(
                    branch.getBranchId(), startDate, endDate, category, sortBy, sortDir)) {
                FnbProductRevenueResponse old = grouped.get(item.itemId());
                if (old == null) grouped.put(item.itemId(), item);
                else grouped.put(item.itemId(), new FnbProductRevenueResponse(item.itemId(), item.itemName(),
                        item.category(), old.soldQuantity() + item.soldQuantity(),
                        old.revenue().add(item.revenue())));
            }
        }
        List<FnbProductRevenueResponse> items = new ArrayList<>(grouped.values());
        Comparator<FnbProductRevenueResponse> comparator = switch (sortBy) {
            case "sold" -> Comparator.comparingLong(FnbProductRevenueResponse::soldQuantity);
            case "revenue" -> Comparator.comparing(FnbProductRevenueResponse::revenue);
            default -> Comparator.comparing(FnbProductRevenueResponse::itemName, String.CASE_INSENSITIVE_ORDER);
        };
        items.sort(comparator);
        if ("desc".equals(sortDir)) Collections.reverse(items);

        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", findBranch(branchId));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("category", category);
        int pageSize = 10;
        int totalRows = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int from = page * pageSize;
        int to = Math.min(from + pageSize, totalRows);
        List<FnbProductRevenueResponse> pageItems = items.subList(from, to);

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("items", pageItems);
        model.addAttribute("allItems", items);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalRows", totalRows);
        model.addAttribute("totalPages", totalPages);
        return "admin/revenue-fnb";
    }

    @GetMapping("/fnb/{itemId}")
    public String showFnbItemTransactions(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String paymentMethod,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        validateBranchId(branchId);
        List<FnbRevenueRowResponse> transactions = new ArrayList<>();
        for (BranchResponse branch : branchesFor(branchId)) {
            try {
                transactions.addAll(revenueReportService.getFnbItemTransactions(branch.getBranchId(), itemId,
                        startDate, endDate, paymentMethod, sortDir));
            } catch (IllegalArgumentException ignored) {
                // Item không có trong kho chi nhánh này; tiếp tục kiểm tra các chi nhánh khác.
            }
        }
        transactions.sort(Comparator.comparing(FnbRevenueRowResponse::transactionTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        if (!"asc".equalsIgnoreCase(sortDir)) Collections.reverse(transactions);

        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil(transactions.size() / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int from = page * pageSize;
        int to = Math.min(from + pageSize, transactions.size());

        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", findBranch(branchId));
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
        return "admin/revenue-fnb-detail";
    }

    private List<RevenueMovieResponse> getMovieRows(Long branchId, LocalDate startDate, LocalDate endDate,
                                                    MovieStatus status, String sortBy, String sortDir) {
        if (branchId != null) {
            return revenueReportService.getMovieReport(branchId, startDate, endDate, status, sortBy, sortDir);
        }
        Map<Long, RevenueMovieResponse> grouped = new LinkedHashMap<>();
        for (BranchResponse branch : branchService.getAllBranches()) {
            for (RevenueMovieResponse movie : revenueReportService.getMovieReport(branch.getBranchId(),
                    startDate, endDate, status, sortBy, sortDir)) {
                RevenueMovieResponse old = grouped.get(movie.movieId());
                if (old == null) grouped.put(movie.movieId(), movie);
                else grouped.put(movie.movieId(), new RevenueMovieResponse(movie.movieId(), movie.movieTitle(),
                        movie.posterUrl(), movie.status(), old.showtimeCount() + movie.showtimeCount(),
                        old.ticketCount() + movie.ticketCount(), old.totalSeats() + movie.totalSeats(),
                        old.revenue().add(movie.revenue())));
            }
        }
        Comparator<RevenueMovieResponse> comparator = switch (sortBy) {
            case "revenue" -> Comparator.comparing(RevenueMovieResponse::revenue);
            case "showtimes" -> Comparator.comparingLong(RevenueMovieResponse::showtimeCount);
            default -> Comparator.comparing(RevenueMovieResponse::movieTitle, String.CASE_INSENSITIVE_ORDER);
        };
        List<RevenueMovieResponse> result = new ArrayList<>(grouped.values());
        result.sort(comparator);
        if ("desc".equals(sortDir)) Collections.reverse(result);
        return result;
    }

    private List<BranchResponse> branchesFor(Long branchId) {
        if (branchId == null) {
            return branchService.getAllBranches();
        }

        return List.of(
                branchService.getAllBranches().stream()
                        .filter(branch -> branch.getBranchId().equals(branchId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại."))
        );
    }

    private BranchResponse findBranch(Long branchId) {
        if (branchId == null) {
            return null;
        }

        return branchService.getAllBranches().stream()
                .filter(branch -> branch.getBranchId().equals(branchId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại."));
    }

    private void validateBranchId(Long branchId) {
        if (branchId != null) {
            findBranch(branchId);
        }
    }
}
