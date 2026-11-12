package fu.cinema.controller.manager;

import fu.cinema.dto.response.*;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Staff;
import fu.cinema.enums.MovieStatus;
import fu.cinema.service.RevenueReportService;
import fu.cinema.service.ShowtimeService;
import fu.cinema.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/manager/revenue")
@RequiredArgsConstructor
public class ManagerRevenueController {

    private final RevenueReportService revenueReportService;
    private final StaffService staffService;
    private final ShowtimeService showtimeService;

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

            @RequestParam(defaultValue = "0") int page,

            Model model
    ) {
        Staff staff = getCurrentManager();

        if (!revenueType.equals("all")
                && !revenueType.equals("tickets")
                && !revenueType.equals("fnb")) {
            revenueType = "all";
        }

        Pageable pageable = PageRequest.of(page, 10);
        RevenueReportResponse report = revenueReportService.getReport(
                staff.getBranch().getBranchId(),
                startDate,
                endDate,
                revenueType,
                pageable
        );

        Page<RevenueTransactionResponse> pageData = report.transactions();

        model.addAttribute("currentBranch", staff.getBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("revenueType", revenueType);
        model.addAttribute("report", report);

        model.addAttribute("transactions", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("pageSize", pageData.getSize());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("totalPages", pageData.getTotalPages());

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
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        if (!List.of("title", "revenue", "showtimes").contains(sortBy)) {
            sortBy = "title";
        }
        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "asc";
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, "title"));

        Page<RevenueMovieResponse> pageData = revenueReportService.getMovieReport(
                branchId, movieName, movieStatus, startDate, endDate, sortBy, sortDir, pageable);
        MovieRevenueSummaryResponse summary = revenueReportService.getMovieRevenueSummary(
                branchId, movieName, movieStatus, startDate, endDate);

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("movieName", movieName);
        model.addAttribute("movieStatus", movieStatus);
        model.addAttribute("movieStatuses", MovieStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        model.addAttribute("movies", pageData.getContent());
        model.addAttribute("items", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("pageSize", pageData.getSize());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("totalPages", pageData.getTotalPages());

        model.addAttribute("totalRevenue", summary.totalRevenue());
        model.addAttribute("totalShowtimes", summary.totalShowtimes());
        model.addAttribute("totalTickets", summary.totalTickets());
        model.addAttribute("totalSeats", summary.totalSeats());

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
            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        if (!"time".equalsIgnoreCase(sortBy) && !"revenue".equalsIgnoreCase(sortBy)) {
            sortBy = "time";
        }
        if (!List.of("asc", "desc").contains(sortDir)) {
            sortDir = "desc";
        }

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        boolean movieBelongsToBranch = showtimeService.isMovieInBranch(branchId, movieId);
        if (!movieBelongsToBranch) {
            return "redirect:/manager/revenue";
        }

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("movieId", movieId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, "startTime"));

        Page<RevenueShowtimeResponse> pageData = revenueReportService.getShowtimeReport(
                branchId, movieId, startDate, endDate, sortBy, sortDir, pageable);

        model.addAttribute("showtimes", pageData.getContent());
        model.addAttribute("items", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("pageSize", pageData.getSize());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("totalPages", pageData.getTotalPages());

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
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Long branchId = getCurrentManagerBranchId();

        Showtime showtime = showtimeService.getShowtimeForBranch(showtimeId, branchId);

        Pageable pageable = PageRequest.of(page, 10);
        var report = revenueReportService.getShowtimeDetail(
                branchId,
                showtimeId,
                startDate,
                endDate,
                paymentMethod,
                pageable
        );

        Page<RevenueTransactionResponse> pageData = report.transactions();

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("showtime", showtime);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("report", report);
        model.addAttribute("totalTicketCount", report.transactionCount());

        model.addAttribute("transactions", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("pageSize", pageData.getSize());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("totalPages", pageData.getTotalPages());

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

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, "booking.payment.transactionTime"));

        Page<FnbRevenueRowResponse> pageData = revenueReportService.getFnbItemTransactions(
                branchId, itemId, startDate, endDate, paymentMethod, sortDir, pageable);

        model.addAttribute("currentBranch", getCurrentManagerBranch());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("itemId", itemId);
        model.addAttribute("rows", pageData.getContent());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("pageSize", pageData.getSize());
        return "manager/revenue-fnb-detail";
    }

    private Staff getCurrentManager() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return staffService.getManagerByUsername(username);
    }

    private Long getCurrentManagerBranchId() {
        return getCurrentManager().getBranch().getBranchId();
    }

    private Branch getCurrentManagerBranch() {
        return getCurrentManager().getBranch();
    }
}
