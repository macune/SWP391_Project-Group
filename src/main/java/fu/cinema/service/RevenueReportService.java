package fu.cinema.service;

import fu.cinema.dto.response.*;
import fu.cinema.enums.MovieStatus;

import java.time.LocalDate;
import java.util.List;

public interface RevenueReportService {

    RevenueReportResponse getReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String revenueType
    );

    List<RevenueMovieResponse> getMovieReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            MovieStatus movieStatus,
            String sortBy,
            String sortDir
    );

    List<RevenueShowtimeResponse> getShowtimeReport(
            Long branchId,
            Long movieId,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir
    );

    RevenueReportResponse getShowtimeDetail(
            Long branchId,
            Long showtimeId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod
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

    List<FnbRevenueRowResponse> getFnbItemTransactions(
            Long branchId,
            Long itemId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String sortDir
    );
}