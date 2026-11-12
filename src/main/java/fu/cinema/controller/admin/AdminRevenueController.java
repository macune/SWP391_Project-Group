package fu.cinema.controller.admin;

import fu.cinema.dto.response.*;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.MovieStatus;
import fu.cinema.service.BranchService;
import fu.cinema.service.RevenueReportService;
import fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRevenueController {

    private final RevenueReportService revenueReportService;
    private final BranchService branchService;
    private final ShowtimeService showtimeService;

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

        BranchResponse currentBranch = branchService.getBranchById(branchId);
        sortBy = List.of("title", "revenue", "showtimes").contains(sortBy) ? sortBy : "title";
        sortDir = List.of("asc", "desc").contains(sortDir) ? sortDir : "asc";

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, "title"));

        Page<RevenueMovieResponse> pageData = revenueReportService.getMovieReportForAdmin(
                branchId, movieName, movieStatus, startDate, endDate, sortBy, sortDir, pageable);
        MovieRevenueSummaryResponse summary = revenueReportService.getMovieRevenueSummaryForAdmin(
                branchId, movieName, movieStatus, startDate, endDate);

        model.addAttribute("items", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("pageSize", pageData.getSize());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", currentBranch);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("movieName", movieName);
        model.addAttribute("movieStatus", movieStatus);
        model.addAttribute("movieStatuses", MovieStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("totalRevenue", summary.totalRevenue());
        model.addAttribute("totalShowtimes", summary.totalShowtimes());
        model.addAttribute("totalTickets", summary.totalTickets());
        model.addAttribute("totalSeats", summary.totalSeats());
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

        BranchResponse currentBranch = branchService.getBranchById(branchId);
        sortBy = List.of("time", "revenue").contains(sortBy) ? sortBy : "time";
        sortDir = List.of("asc", "desc").contains(sortDir) ? sortDir : "desc";

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, "startTime"));

        AdminShowtimeRevenueResponse data = revenueReportService.getShowtimeReportForAdmin(
                branchId, movieId, startDate, endDate, sortBy, sortDir, pageable);
        Page<RevenueShowtimeResponse> pageData = data.showtimes();

        model.addAttribute("items", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("pageSize", pageData.getSize());
        model.addAttribute("totalRows", pageData.getTotalElements());
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", currentBranch);
        model.addAttribute("movieId", movieId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("showtimes", pageData.getContent());
        model.addAttribute("showtimeBranchNames", data.showtimeBranchNames());
        return "admin/revenue-showtimes";
    }

    @GetMapping("/showtime/{showtimeId}")
    public String showShowtimeDetail(
            @PathVariable Long showtimeId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "all") String paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        branchService.getBranchById(branchId);
        Showtime showtime = showtimeService.getShowtimeForBranch(showtimeId, branchId);
        Long actualBranchId = showtime.getHall().getBranch().getBranchId();

        Pageable pageable = PageRequest.of(page, 10);
        RevenueReportResponse report = revenueReportService.getShowtimeDetail(
                actualBranchId, showtimeId, startDate, endDate, paymentMethod, pageable);

        Page<RevenueTransactionResponse> pageData = report.transactions();

        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", branchService.getBranchById(actualBranchId));
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

        BranchResponse currentBranch = branchService.getBranchById(branchId);
        category = List.of("ALL", "FOOD", "DRINK", "COMBO").contains(category) ? category : "ALL";
        sortBy = List.of("name", "sold", "revenue").contains(sortBy) ? sortBy : "name";
        sortDir = List.of("asc", "desc").contains(sortDir) ? sortDir : "asc";

        List<FnbProductRevenueResponse> items = revenueReportService.getFnbProductRevenueForAdmin(
                branchId, startDate, endDate, category, sortBy, sortDir);

        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", currentBranch);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("category", category);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("items", items);
        model.addAttribute("allItems", items);
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

        BranchResponse currentBranch = branchService.getBranchById(branchId);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, "booking.payment.transactionTime"));

        AdminFnbItemTransactionsResponse data = revenueReportService.getFnbItemTransactionsForAdmin(
                branchId, itemId, startDate, endDate, paymentMethod, sortDir, pageable);

        Page<FnbRevenueRowResponse> pageData = data.rows();

        model.addAttribute("transactionBranchNames", data.transactionBranchNames());
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("currentBranch", currentBranch);
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
        return "admin/revenue-fnb-detail";
    }
}
