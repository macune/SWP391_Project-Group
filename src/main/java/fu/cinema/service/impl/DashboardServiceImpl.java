package fu.cinema.service.impl;

import fu.cinema.dto.response.*;
import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.entity.BookingFnB;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.exception.AppException;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.repository.TicketRepository;
import fu.cinema.service.BranchStockService;
import fu.cinema.service.DashboardService;
import fu.cinema.service.PublicMovieService;
import fu.cinema.service.RevenueReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardServiceImpl implements DashboardService {

    ShowtimeRepository showtimeRepository;
    RevenueReportService revenueReportService;
    TicketRepository ticketRepository;
    BookingRepository bookingRepository;
    PublicMovieService movieService;
    AccountRepository accountRepository;
    BranchStockService branchStockService;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Long branchId, String period) {
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

        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();

        // Lấy suất chiếu theo chi nhánh hoặc toàn chuỗi
        List<Showtime> allShowtimes = branchId == null
                ? showtimeRepository.findAll()
                : showtimeRepository.findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);

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

            if (showtime.getHall() != null && showtime.getHall().getCapacity() != null) {
                seatCapacity += showtime.getHall().getCapacity();
            }
        }

        double occupancyRate = seatCapacity == 0 ? 0 : ticketCount * 100.0 / seatCapacity;

        // Doanh thu
        RevenueReportResponse report = revenueReportService.getReport(branchId, startDate, today, "all");
        BigDecimal totalRevenue = report.totalRevenue() == null ? BigDecimal.ZERO : report.totalRevenue();

        boolean dailyChart = "week".equals(period) || "month".equals(period);
        Map<String, BigDecimal> revenueByPeriod = new LinkedHashMap<>();

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        if (dailyChart && startDate != null) {
            for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
                revenueByPeriod.put(d.format(dayFormatter), BigDecimal.ZERO);
            }
        } else {
            LocalDate firstMonth = startDate == null
                    ? (report.transactions() == null || report.transactions().isEmpty()
                    ? today.withDayOfMonth(1)
                    : report.transactions().stream()
                    .filter(t -> t.transactionTime() != null)
                    .map(t -> t.transactionTime().toLocalDate().withDayOfMonth(1))
                    .min(LocalDate::compareTo)
                    .orElse(today.withDayOfMonth(1)))
                    : startDate.withDayOfMonth(1);

            for (LocalDate d = firstMonth; !d.isAfter(today); d = d.plusMonths(1)) {
                revenueByPeriod.put(d.format(monthFormatter), BigDecimal.ZERO);
            }
        }

        if (report.transactions() != null) {
            for (var transaction : report.transactions()) {
                if (transaction.transactionTime() == null) continue;

                LocalDate date = transaction.transactionTime().toLocalDate();
                String key = dailyChart
                        ? date.format(dayFormatter)
                        : date.withDayOfMonth(1).format(monthFormatter);

                if (revenueByPeriod.containsKey(key)) {
                    BigDecimal amount = transaction.amount() == null ? BigDecimal.ZERO : transaction.amount();
                    revenueByPeriod.merge(key, amount, BigDecimal::add);
                }
            }
        }

        // Top 5 phim theo số vé hợp lệ
        Map<Long, DashboardTopMovieResponse> movieStats = new HashMap<>();

        for (Showtime showtime : periodShowtimes) {
            if (showtime.getTickets() == null || showtime.getMovie() == null) continue;

            long sold = showtime.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED
                            || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();

            if (sold == 0) continue;

            Long movieId = showtime.getMovie().getMovieId();

            movieStats.compute(movieId, (id, existing) -> {
                long currentTickets = (existing == null) ? 0L : existing.tickets();
                return new DashboardTopMovieResponse(
                        id,
                        showtime.getMovie().getTitle(),
                        showtime.getMovie().getPosterUrl(),
                        currentTickets + sold
                );
            });
        }

        List<DashboardTopMovieResponse> topMovies = movieStats.values().stream()
                .sorted(Comparator.comparingLong(DashboardTopMovieResponse::tickets).reversed())
                .limit(5)
                .toList();

        // 5 suất chiếu sắp tới
        LocalDateTime now = LocalDateTime.now();

        List<DashboardUpcomingShowtimeResponse> upcomingShowtimes = allShowtimes.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> !s.getStartTime().isBefore(now))
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .limit(5)
                .map(s -> new DashboardUpcomingShowtimeResponse(
                        s.getMovie() != null ? s.getMovie().getTitle() : null,
                        s.getMovie() != null ? s.getMovie().getPosterUrl() : null,
                        s.getHall() != null ? s.getHall().getHallName() : null,
                        s.getStartTime(),
                        s.getStatus()
                ))
                .toList();

        String periodLabel = switch (period) {
            case "week" -> "7 ngày gần nhất";
            case "month" -> "1 tháng gần nhất";
            case "year" -> "1 năm gần nhất";
            default -> "Toàn bộ dữ liệu";
        };

        return new DashboardResponse(
                period,
                periodLabel,
                totalRevenue,
                ticketCount,
                periodShowtimes.size(),
                occupancyRate,
                new ArrayList<>(revenueByPeriod.keySet()),
                revenueByPeriod.values().stream().map(BigDecimal::doubleValue).toList(),
                topMovies,
                upcomingShowtimes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.Customer getCustomerDashboardData(
            String username,
            int page,
            int pageSize,
            LocalDate showDate,
            Integer month,
            Integer year,
            LocalDate bookingDate,
            Integer bookingMonth,
            Integer bookingYear
    ) {
        long upcomingTicketCount = ticketRepository.countByBooking_Account_UsernameAndBooking_StatusAndStatusInAndShowtime_StartTimeAfter(
                username, BookingStatus.CONFIRMED, List.of(TicketStatus.BOOKED, TicketStatus.CHECKED_IN), LocalDateTime.now());

        long completedBookingCount = bookingRepository.countByAccount_UsernameAndStatus(username, BookingStatus.CONFIRMED);

        LocalDateTime showFrom = null;
        LocalDateTime showTo = null;

        if (showDate != null) {
            showFrom = showDate.atStartOfDay();
            showTo = showDate.plusDays(1).atStartOfDay();
        } else {
            if (month != null && month >= 1 && month <= 12) {
                int selectedYear = year != null ? year : LocalDate.now().getYear();
                LocalDateTime monthFrom = LocalDate.of(selectedYear, month, 1).atStartOfDay();
                LocalDateTime monthTo = monthFrom.toLocalDate().plusMonths(1).atStartOfDay();
                showFrom = monthFrom;
                showTo = monthTo;
            } else if (year != null && year >= 1900 && year <= 9998) {
                showFrom = LocalDate.of(year, 1, 1).atStartOfDay();
                showTo = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            }
        }

        LocalDateTime bookingFrom = null;
        LocalDateTime bookingTo = null;

        if (bookingDate != null) {
            bookingFrom = bookingDate.atStartOfDay();
            bookingTo = bookingDate.plusDays(1).atStartOfDay();
        } else {
            if (bookingMonth != null && bookingMonth >= 1 && bookingMonth <= 12) {
                int selectedYear = bookingYear != null ? bookingYear : LocalDate.now().getYear();
                YearMonth selectedMonth = YearMonth.of(selectedYear, bookingMonth);
                bookingFrom = selectedMonth.atDay(1).atStartOfDay();
                bookingTo = selectedMonth.plusMonths(1).atDay(1).atStartOfDay();
            } else if (bookingYear != null && bookingYear >= 1900 && bookingYear <= 9998) {
                bookingFrom = LocalDate.of(bookingYear, 1, 1).atStartOfDay();
                bookingTo = LocalDate.of(bookingYear + 1, 1, 1).atStartOfDay();
            }
        }

        int validPage = Math.max(0, page);
        int validPageSize = pageSize > 0 ? pageSize : 9;

        Page<Ticket> ticketPage = ticketRepository.searchCustomerTickets(
                username, TicketStatus.LOCKED, showFrom, showTo, bookingFrom, bookingTo, PageRequest.of(validPage, validPageSize)
        );

        List<Object[]> topRows = ticketRepository.findTopMoviesByBookingCount(
                BookingStatus.CONFIRMED, List.of(TicketStatus.BOOKED, TicketStatus.CHECKED_IN), MovieStatus.NOW_SHOWING, PageRequest.of(0, 4)
        );

        List<DashboardResponse.Customer.TopMovie> topMovies = topRows.stream()
                .map(row -> new DashboardResponse.Customer.TopMovie(
                        movieService.getMovieDetail((Long) row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        return new DashboardResponse.Customer(
                upcomingTicketCount,
                completedBookingCount,
                ticketPage,
                topMovies,
                showDate,
                month,
                year,
                bookingDate,
                bookingMonth,
                bookingYear
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.Staff getStaffDashboardData(String username) {
        if (username == null || username.isBlank()) {
            throw new AppException("Không tìm thấy thông tin đăng nhập.");
        }

        Account staffAcc = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Tài khoản không tồn tại"));

        if (staffAcc.getStaff() == null || staffAcc.getStaff().getBranch() == null) {
            throw new AppException("Tài khoản nhân viên chưa được gán chi nhánh.");
        }

        Branch branch = staffAcc.getStaff().getBranch();
        Long branchId = branch.getBranchId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<Showtime> todayShowtimes = showtimeRepository
                .findByHall_Branch_BranchIdAndStartTimeBetweenAndStatusNotOrderByStartTimeAsc(
                        branchId, startOfDay, endOfDay, ShowtimeStatus.CANCELLED);

        // Tính doanh thu vé và bắp nước từ các suất chiếu diễn ra trong ngày hôm nay
        BigDecimal ticketRevenue = BigDecimal.ZERO;
        BigDecimal fnbRevenue = BigDecimal.ZERO;
        Set<Long> processedBookingIds = new HashSet<>();

        for (Showtime s : todayShowtimes) {
            if (s.getTickets() != null) {
                for (Ticket t : s.getTickets()) {
                    if (t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN) {
                        if (t.getPrice() != null) {
                            ticketRevenue = ticketRevenue.add(t.getPrice());
                        }
                        if (t.getBooking() != null && t.getBooking().getStatus() == BookingStatus.CONFIRMED) {
                            Booking b = t.getBooking();
                            if (processedBookingIds.add(b.getBookingId())) {
                                if (b.getBookingFnbs() != null) {
                                    for (BookingFnB fnb : b.getBookingFnbs()) {
                                        if (fnb.getSubtotal() != null) {
                                            fnbRevenue = fnbRevenue.add(fnb.getSubtotal());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bắp nước bán lẻ tại quầy POS trong ngày hôm nay (đơn không có vé)
        List<Booking> todayPosBookings = bookingRepository.findByStatusAndBookingTimeBetween(
                BookingStatus.CONFIRMED, startOfDay, endOfDay);
        for (Booking b : todayPosBookings) {
            if ((b.getTickets() == null || b.getTickets().isEmpty())
                    && b.getAccount() != null && b.getAccount().getStaff() != null && b.getAccount().getStaff().getBranch() != null
                    && branchId.equals(b.getAccount().getStaff().getBranch().getBranchId())) {
                if (processedBookingIds.add(b.getBookingId())) {
                    if (b.getBookingFnbs() != null && !b.getBookingFnbs().isEmpty()) {
                        for (BookingFnB fnb : b.getBookingFnbs()) {
                            if (fnb.getSubtotal() != null) {
                                fnbRevenue = fnbRevenue.add(fnb.getSubtotal());
                            }
                        }
                    } else if (b.getTotalAmount() != null) {
                        fnbRevenue = fnbRevenue.add(b.getTotalAmount());
                    }
                }
            }
        }

        BigDecimal dailyRevenue = ticketRevenue.add(fnbRevenue);

        var menuItems = branchStockService.getStockByBranch(branchId);
        long availableItems = menuItems.stream()
                .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()))
                .count();

        LocalDateTime now = LocalDateTime.now();
        List<Showtime> upcomingShowtimes = todayShowtimes.stream()
                .filter(st -> st.getStartTime() != null && !st.getStartTime().isBefore(now))
                .limit(5)
                .toList();

        return new DashboardResponse.Staff(
                branch.getBranchName(),
                today,
                dailyRevenue,
                ticketRevenue,
                fnbRevenue,
                todayShowtimes.size(),
                availableItems,
                upcomingShowtimes
        );
    }
}
