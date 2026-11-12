package fu.cinema.service;

import fu.cinema.dto.response.*;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RevenueReportService {

    RevenueReportResponse getReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String revenueType
    );

    RevenueReportResponse getReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String revenueType,
            Pageable pageable
    );

    Page<RevenueMovieResponse> getMovieReport(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    );

    Page<RevenueMovieResponse> getMovieReportForAdmin(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    );

    MovieRevenueSummaryResponse getMovieRevenueSummary(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate
    );

    MovieRevenueSummaryResponse getMovieRevenueSummaryForAdmin(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate
    );

    Page<RevenueShowtimeResponse> getShowtimeReport(
            Long branchId,
            Long movieId,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    );

    AdminShowtimeRevenueResponse getShowtimeReportForAdmin(
            Long branchId,
            Long movieId,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    );

    RevenueReportResponse getShowtimeDetail(
            Long branchId,
            Long showtimeId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            Pageable pageable
    );

    List<FnbRevenueRowResponse> getFnbRevenueRows(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<FnbProductRevenueResponse> getFnbProductRevenue(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String sortBy,
            String sortDir
    );

    List<FnbProductRevenueResponse> getFnbProductRevenueForAdmin(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String sortBy,
            String sortDir
    );

    Page<FnbRevenueRowResponse> getFnbItemTransactions(
            Long branchId,
            Long itemId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String sortDir,
            Pageable pageable
    );

    AdminFnbItemTransactionsResponse getFnbItemTransactionsForAdmin(
            Long branchId,
            Long itemId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String sortDir,
            Pageable pageable
    );

    ShowtimeAnalyticsResponse getShowtimeAnalytics(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            Long movieId,
            ShowtimeStatus status,
            String sortBy,
            String sortDir,
            int page,
            int pageSize
    );
}
