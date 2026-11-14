package fu.cinema.service.impl;

import fu.cinema.dto.response.*;
import fu.cinema.entity.*;
import fu.cinema.enums.PaymentMethod;
import fu.cinema.enums.PaymentStatus;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.*;
import fu.cinema.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueReportServiceImpl implements RevenueReportService {

    private final BranchRepository branchRepository;
    private final BranchStockRepository branchStockRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final BookingFnBRepository bookingFnBRepository;
    private final FnBItemRepository fnbItemRepository;

    @Override
    public RevenueReportResponse getReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String revenueType
    ) {
        return getReport(branchId, startDate, endDate, revenueType, org.springframework.data.domain.PageRequest.of(0, 10));
    }

    @Override
    public RevenueReportResponse getReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String revenueType,
            Pageable pageable
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        List<Payment> payments = paymentRepository.findSuccessfulPaymentsForAdmin(
                branchId, PaymentStatus.SUCCESS, startTime, endTime);

        List<RevenueTransactionResponse> allTransactions = payments.stream()
                .map(payment -> {
                    BigDecimal ticketRevenue = payment.getBooking().getTickets().stream()
                            .filter(ticket -> ticket.getStatus() != TicketStatus.CANCELLED)
                            .map(Ticket::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal fnbRevenue = payment.getBooking().getBookingFnbs().stream()
                            .map(BookingFnB::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new RevenueTransactionResponse(
                            payment.getPaymentId(),
                            payment.getBooking().getBookingId(),
                            payment.getTransactionTime(),
                            payment.getPaymentMethod().name(),
                            payment.getAmount(),
                            ticketRevenue,
                            fnbRevenue,
                            payment.getBooking().getTickets().stream()
                                    .filter(ticket -> ticket.getStatus() == TicketStatus.BOOKED
                                            || ticket.getStatus() == TicketStatus.CHECKED_IN)
                                    .count()
                    );
                })
                .filter(transaction -> {
                    if ("tickets".equalsIgnoreCase(revenueType)) {
                        return transaction.ticketRevenue().compareTo(BigDecimal.ZERO) > 0;
                    }
                    if ("fnb".equalsIgnoreCase(revenueType)) {
                        return transaction.fnbRevenue().compareTo(BigDecimal.ZERO) > 0;
                    }
                    return true;
                })
                .toList();

        BigDecimal totalRevenue = allTransactions.stream()
                .map(RevenueTransactionResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTicketRevenue = allTransactions.stream()
                .map(RevenueTransactionResponse::ticketRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFnbRevenue = allTransactions.stream()
                .map(RevenueTransactionResponse::fnbRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTransactions.size());
        List<RevenueTransactionResponse> pageList = (start > allTransactions.size())
                ? Collections.emptyList()
                : allTransactions.subList(start, end);

        Page<RevenueTransactionResponse> transactionPage = new PageImpl<>(pageList, pageable, allTransactions.size());

        return new RevenueReportResponse(
                totalRevenue,
                totalTicketRevenue,
                totalFnbRevenue,
                allTransactions.size(),
                transactionPage
        );
    }

    @Override
    public Page<RevenueMovieResponse> getMovieReport(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    ) {
        String searchName = (movieName == null || movieName.isBlank()) ? null : movieName.trim();
        Page<Movie> moviePage = movieRepository.findMoviesForRevenueReport(movieStatus, searchName, pageable);

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        return moviePage.map(movie -> calculateMovieMetrics(movie, branchId, startTime, endTime));
    }

    @Override
    public Page<RevenueMovieResponse> getMovieReportForAdmin(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    ) {
        return getMovieReport(branchId, movieName, movieStatus, startDate, endDate, sortBy, sortDir, pageable);
    }

    @Override
    public MovieRevenueSummaryResponse getMovieRevenueSummary(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String searchName = (movieName == null || movieName.isBlank()) ? null : movieName.trim();
        List<Movie> allMovies = movieRepository.findAll().stream()
                .filter(m -> movieStatus == null || m.getStatus() == movieStatus)
                .filter(m -> searchName == null || m.getTitle().toLowerCase().contains(searchName.toLowerCase()))
                .toList();

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalShowtimes = 0;
        long totalTickets = 0;
        long totalSeats = 0;

        for (Movie m : allMovies) {
            RevenueMovieResponse res = calculateMovieMetrics(m, branchId, startTime, endTime);
            totalRevenue = totalRevenue.add(res.revenue());
            totalShowtimes += res.showtimeCount();
            totalTickets += res.ticketCount();
            totalSeats += res.totalSeats();
        }

        return new MovieRevenueSummaryResponse(totalRevenue, totalShowtimes, totalTickets, totalSeats);
    }

    @Override
    public MovieRevenueSummaryResponse getMovieRevenueSummaryForAdmin(
            Long branchId,
            String movieName,
            MovieStatus movieStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getMovieRevenueSummary(branchId, movieName, movieStatus, startDate, endDate);
    }

    private RevenueMovieResponse calculateMovieMetrics(
            Movie movie, Long branchId, LocalDateTime startTime, LocalDateTime endTime
    ) {
        List<Showtime> showtimes = movie.getShowtimes().stream()
                .filter(s -> branchId == null || (s.getHall() != null && s.getHall().getBranch() != null
                        && branchId.equals(s.getHall().getBranch().getBranchId())))
                .filter(s -> startTime == null || !s.getStartTime().isBefore(startTime))
                .filter(s -> endTime == null || s.getStartTime().isBefore(endTime))
                .toList();

        long showtimeCount = showtimes.size();
        long totalSeats = 0;
        long ticketCount = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (Showtime s : showtimes) {
            if (s.getHall() != null && s.getHall().getSeats() != null) {
                totalSeats += s.getHall().getSeats().size();
            }
            if (s.getTickets() != null) {
                for (Ticket t : s.getTickets()) {
                    if (t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN) {
                        ticketCount++;
                        if (t.getPrice() != null) {
                            revenue = revenue.add(t.getPrice());
                        }
                    }
                }
            }
        }

        return new RevenueMovieResponse(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getPosterUrl(),
                movie.getStatus(),
                showtimeCount,
                ticketCount,
                totalSeats,
                revenue
        );
    }

    @Override
    public Page<RevenueShowtimeResponse> getShowtimeReport(
            Long branchId,
            Long movieId,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    ) {
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        Page<Showtime> showtimePage = showtimeRepository.findShowtimesForRevenueReport(
                movieId, branchId, startTime, endTime, pageable);

        return showtimePage.map(showtime -> {
            Long sId = showtime.getShowtimeId();
            long totalSeats = showtime.getHall() != null && showtime.getHall().getSeats() != null
                    ? showtime.getHall().getSeats().size() : 0L;

            long ticketCount = 0L;
            BigDecimal revenue = BigDecimal.ZERO;

            if (showtime.getTickets() != null) {
                for (Ticket ticket : showtime.getTickets()) {
                    if (ticket.getStatus() == TicketStatus.BOOKED || ticket.getStatus() == TicketStatus.CHECKED_IN) {
                        ticketCount++;
                        if (ticket.getPrice() != null) {
                            revenue = revenue.add(ticket.getPrice());
                        }
                    }
                }
            }

            return new RevenueShowtimeResponse(
                    sId,
                    showtime.getMovie() != null ? showtime.getMovie().getTitle() : "",
                    showtime.getHall() != null ? showtime.getHall().getHallName() : "",
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    ticketCount,
                    revenue,
                    totalSeats
            );
        });
    }

    @Override
    public AdminShowtimeRevenueResponse getShowtimeReportForAdmin(
            Long branchId,
            Long movieId,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            Pageable pageable
    ) {
        Page<RevenueShowtimeResponse> pageData = getShowtimeReport(
                branchId, movieId, startDate, endDate, sortBy, sortDir, pageable);

        Map<Long, String> showtimeBranchNames = new HashMap<>();
        for (RevenueShowtimeResponse row : pageData.getContent()) {
            Showtime s = showtimeRepository.findById(row.showtimeId()).orElse(null);
            if (s != null && s.getHall() != null && s.getHall().getBranch() != null) {
                showtimeBranchNames.put(row.showtimeId(), s.getHall().getBranch().getBranchName());
            }
        }

        return new AdminShowtimeRevenueResponse(pageData, showtimeBranchNames);
    }

    @Override
    public RevenueReportResponse getShowtimeDetail(
            Long branchId,
            Long showtimeId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            Pageable pageable
    ) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));

        if (branchId != null && (showtime.getHall() == null
                || showtime.getHall().getBranch() == null
                || !showtime.getHall().getBranch().getBranchId().equals(branchId))) {
            throw new IllegalArgumentException("Bạn không có quyền xem báo cáo của suất chiếu này.");
        }

        String method = paymentMethod == null ? "ALL" : paymentMethod.trim().toUpperCase();
        PaymentMethod methodEnum = "ALL".equalsIgnoreCase(method) ? null : PaymentMethod.valueOf(method);

        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        List<Payment> allPayments = paymentRepository.findAllPaymentsByShowtime(
                showtimeId, PaymentStatus.SUCCESS, methodEnum, from, toExclusive);

        BigDecimal ticketRevenue = BigDecimal.ZERO;
        BigDecimal fnbRevenue = BigDecimal.ZERO;
        long totalTicketCount = 0;

        for (Payment payment : allPayments) {
            if (payment.getBooking() == null) continue;
            BigDecimal tAmt = payment.getBooking().getTickets().stream()
                    .filter(t -> t.getShowtime() != null && showtimeId.equals(t.getShowtime().getShowtimeId()))
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                    .map(Ticket::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal fAmt = payment.getBooking().getBookingFnbs().stream()
                    .map(BookingFnB::getSubtotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long tCount = payment.getBooking().getTickets().stream()
                    .filter(t -> t.getShowtime() != null && showtimeId.equals(t.getShowtime().getShowtimeId()))
                    .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                    .count();

            ticketRevenue = ticketRevenue.add(tAmt);
            fnbRevenue = fnbRevenue.add(fAmt);
            totalTicketCount += tCount;
        }

        Page<Payment> paymentPage = paymentRepository.findPaymentsByShowtime(
                showtimeId, PaymentStatus.SUCCESS, methodEnum, from, toExclusive, pageable);

        Page<RevenueTransactionResponse> transactionPage = paymentPage.map(payment -> {
            Booking booking = payment.getBooking();
            List<Ticket> matchingTickets = booking == null ? Collections.emptyList() :
                    booking.getTickets().stream()
                            .filter(t -> t.getShowtime() != null && showtimeId.equals(t.getShowtime().getShowtimeId()))
                            .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                            .toList();

            BigDecimal ticketAmt = matchingTickets.stream()
                    .map(Ticket::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal fnbAmt = booking == null ? BigDecimal.ZERO :
                    booking.getBookingFnbs().stream()
                            .map(BookingFnB::getSubtotal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new RevenueTransactionResponse(
                    payment.getPaymentId(),
                    booking != null ? booking.getBookingId() : null,
                    payment.getTransactionTime(),
                    payment.getPaymentMethod() == null ? "N/A" : payment.getPaymentMethod().name(),
                    payment.getAmount() != null ? payment.getAmount() : ticketAmt.add(fnbAmt),
                    ticketAmt,
                    fnbAmt,
                    matchingTickets.size()
            );
        });

        BigDecimal totalRevenue = ticketRevenue.add(fnbRevenue);

        return new RevenueReportResponse(
                totalRevenue,
                ticketRevenue,
                fnbRevenue,
                allPayments.size(),
                transactionPage
        );
    }

    @Override
    public List<FnbRevenueRowResponse> getFnbRevenueRows(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        return paymentRepository.findSuccessfulPaymentsByBranchAndTime(
                        branchId, PaymentStatus.SUCCESS, startTime, endTime
                )
                .stream()
                .filter(payment -> payment.getBooking() != null)
                .flatMap(payment -> payment.getBooking().getBookingFnbs().stream()
                        .map(item -> new FnbRevenueRowResponse(
                                item.getFnbItem().getItemId(),
                                payment.getPaymentId(),
                                payment.getTransactionTime(),
                                payment.getPaymentMethod() == null ? "N/A" : payment.getPaymentMethod().name(),
                                item.getFnbItem().getCategory().name(),
                                item.getFnbItem().getName(),
                                item.getQuantity(),
                                item.getSubtotal()
                        )))
                .sorted(Comparator.comparing(
                        FnbRevenueRowResponse::transactionTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    @Override
    public List<FnbProductRevenueResponse> getFnbProductRevenue(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String sortBy,
            String sortDir
    ) {
        List<FnbRevenueRowResponse> rows = getFnbRevenueRows(branchId, startDate, endDate);

        Map<Long, List<FnbRevenueRowResponse>> rowsByItem = rows.stream()
                .collect(Collectors.groupingBy(FnbRevenueRowResponse::itemId));

        Comparator<FnbProductRevenueResponse> comparator = switch (sortBy == null ? "name" : sortBy) {
            case "sold" -> Comparator.comparingLong(FnbProductRevenueResponse::soldQuantity);
            case "revenue" -> Comparator.comparing(FnbProductRevenueResponse::revenue);
            default -> Comparator.comparing(FnbProductRevenueResponse::itemName, String.CASE_INSENSITIVE_ORDER);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return fnbItemRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()) || rowsByItem.containsKey(item.getItemId()))
                .map(item -> {
                    List<FnbRevenueRowResponse> itemRows = rowsByItem.getOrDefault(item.getItemId(), Collections.emptyList());

                    long sold = itemRows.stream()
                            .mapToLong(row -> row.quantity() == null ? 0 : row.quantity())
                            .sum();

                    BigDecimal revenue = itemRows.stream()
                            .map(FnbRevenueRowResponse::subtotal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new FnbProductRevenueResponse(
                            item.getItemId(),
                            item.getName(),
                            item.getCategory().name(),
                            sold,
                            revenue
                    );
                })
                .filter(item -> "ALL".equalsIgnoreCase(category) || item.category().equalsIgnoreCase(category))
                .sorted(comparator)
                .toList();
    }

    @Override
    public List<FnbProductRevenueResponse> getFnbProductRevenueForAdmin(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String sortBy,
            String sortDir
    ) {
        if (branchId != null) {
            return getFnbProductRevenue(branchId, startDate, endDate, category, sortBy, sortDir);
        }

        Map<Long, FnbProductRevenueResponse> grouped = new LinkedHashMap<>();
        for (Branch branch : branchesFor(null)) {
            for (FnbProductRevenueResponse item : getFnbProductRevenue(
                    branch.getBranchId(), startDate, endDate, category, sortBy, sortDir)) {
                FnbProductRevenueResponse old = grouped.get(item.itemId());
                if (old == null) {
                    grouped.put(item.itemId(), item);
                } else {
                    grouped.put(item.itemId(), new FnbProductRevenueResponse(
                            item.itemId(),
                            item.itemName(),
                            item.category(),
                            old.soldQuantity() + item.soldQuantity(),
                            old.revenue().add(item.revenue())
                    ));
                }
            }
        }
        List<FnbProductRevenueResponse> items = new ArrayList<>(grouped.values());
        Comparator<FnbProductRevenueResponse> comparator = switch (sortBy == null ? "name" : sortBy) {
            case "sold" -> Comparator.comparingLong(FnbProductRevenueResponse::soldQuantity);
            case "revenue" -> Comparator.comparing(FnbProductRevenueResponse::revenue);
            default -> Comparator.comparing(FnbProductRevenueResponse::itemName, String.CASE_INSENSITIVE_ORDER);
        };
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        items.sort(comparator);
        return items;
    }

    @Override
    public Page<FnbRevenueRowResponse> getFnbItemTransactions(
            Long branchId,
            Long itemId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String sortDir,
            Pageable pageable
    ) {
        if (!fnbItemRepository.existsById(itemId)) {
            throw new IllegalArgumentException("Món F&B không tồn tại trong hệ thống.");
        }
        if (branchId != null && !branchRepository.existsById(branchId)) {
            throw new IllegalArgumentException("Chi nhánh không tồn tại.");
        }

        PaymentMethod methodEnum = null;
        if (paymentMethod != null && !"ALL".equalsIgnoreCase(paymentMethod)) {
            methodEnum = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        }

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        Page<BookingFnB> fnbPage = bookingFnBRepository.findFnbTransactions(
                itemId, PaymentStatus.SUCCESS, branchId, methodEnum, startTime, endTime, pageable);

        return fnbPage.map(bf -> {
            Payment payment = bf.getBooking() != null ? bf.getBooking().getPayment() : null;
            return new FnbRevenueRowResponse(
                    bf.getFnbItem().getItemId(),
                    payment != null ? payment.getPaymentId() : null,
                    payment != null ? payment.getTransactionTime() : null,
                    payment != null && payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "N/A",
                    bf.getFnbItem().getCategory() != null ? bf.getFnbItem().getCategory().name() : "",
                    bf.getFnbItem().getName(),
                    bf.getQuantity(),
                    bf.getSubtotal()
            );
        });
    }

    @Override
    public AdminFnbItemTransactionsResponse getFnbItemTransactionsForAdmin(
            Long branchId,
            Long itemId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String sortDir,
            Pageable pageable
    ) {
        Page<FnbRevenueRowResponse> pageData = getFnbItemTransactions(
                branchId, itemId, startDate, endDate, paymentMethod, sortDir, pageable);

        Map<Long, String> transactionBranchNames = new HashMap<>();
        for (FnbRevenueRowResponse row : pageData.getContent()) {
            if (row.paymentId() != null) {
                Payment p = paymentRepository.findById(row.paymentId()).orElse(null);
                if (p != null && p.getBooking() != null && !p.getBooking().getTickets().isEmpty()) {
                    Ticket t = p.getBooking().getTickets().get(0);
                    if (t.getShowtime() != null && t.getShowtime().getHall() != null && t.getShowtime().getHall().getBranch() != null) {
                        transactionBranchNames.put(row.paymentId(), t.getShowtime().getHall().getBranch().getBranchName());
                    }
                }
            }
        }

        return new AdminFnbItemTransactionsResponse(pageData, transactionBranchNames);
    }

    private List<Branch> branchesFor(Long branchId) {
        if (branchId == null) {
            return branchRepository.findAll();
        }
        return List.of(
                branchRepository.findById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại."))
        );
    }

    @Override
    public ShowtimeAnalyticsResponse getShowtimeAnalytics(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            Long movieId,
            ShowtimeStatus status,
            String sortBy,
            String sortDir,
            int page,
            int pageSize
    ) {
        String dateError = null;
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (endDate.isBefore(startDate)) {
            dateError = "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.";
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

        List<Showtime> allShowtimes = (branchId == null)
                ? showtimeRepository.findAll()
                : showtimeRepository.findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId);

        List<Showtime> branchShowtimes = allShowtimes.stream()
                .filter(s -> s.getHall() != null && s.getHall().getBranch() != null)
                .toList();

        Map<Long, Movie> uniqueMovies = new LinkedHashMap<>();
        branchShowtimes.forEach(s -> {
            if (s.getMovie() != null) {
                uniqueMovies.putIfAbsent(s.getMovie().getMovieId(), s.getMovie());
            }
        });

        List<Showtime> filteredShowtimes = allShowtimes.stream()
                .filter(s -> s.getHall() != null && s.getHall().getBranch() != null && s.getMovie() != null)
                .filter(s -> s.getStartTime() != null && !s.getStartTime().isBefore(from) && s.getStartTime().isBefore(to))
                .filter(s -> movieId == null || s.getMovie().getMovieId().equals(movieId))
                .filter(s -> status == null || s.getStatus() == status)
                .toList();

        Comparator<Showtime> comparator = switch (sortBy) {
            case "time" -> Comparator.comparing(Showtime::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()));
            case "revenue" -> Comparator.comparing(this::getShowtimeRevenue);
            default -> Comparator.comparing(s -> s.getMovie().getTitle(), String.CASE_INSENSITIVE_ORDER);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        List<Showtime> sortedShowtimes = filteredShowtimes.stream()
                .sorted(comparator)
                .toList();

        long totalTickets = 0;
        long totalSeats = 0;
        BigDecimal ticketRevenue = BigDecimal.ZERO;

        List<ShowtimeAnalyticsRowResponse> showtimeRows = new ArrayList<>();

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

        for (Showtime s : sortedShowtimes) {
            long soldTickets = 0;
            if (s.getTickets() != null) {
                soldTickets = s.getTickets().stream()
                        .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                        .count();
            }

            BigDecimal revenue = getShowtimeRevenue(s);
            long capacity = (s.getHall() != null && s.getHall().getCapacity() != null)
                    ? s.getHall().getCapacity() : 0L;
            double occupancy = capacity == 0 ? 0.0 : soldTickets * 100.0 / capacity;

            totalTickets += soldTickets;
            totalSeats += capacity;
            ticketRevenue = ticketRevenue.add(revenue);

            showtimeRows.add(new ShowtimeAnalyticsRowResponse(
                    s.getMovie().getTitle(),
                    s.getHall().getHallName(),
                    s.getHall().getBranch().getBranchName(),
                    s.getStartTime(),
                    s.getEndTime(),
                    s.getStatus(),
                    soldTickets,
                    capacity,
                    occupancy,
                    revenue
            ));

            int hour = s.getStartTime().getHour();
            int bucket = hour / 2;
            if (bucket >= 3 && bucket <= 11) {
                String label = String.format("%02d–%02dh", bucket * 2, bucket * 2 + 2);
                if (ticketsByHour.containsKey(label)) {
                    ticketsByHour.merge(label, soldTickets, Long::sum);
                }
            }

            ticketsByDay.merge(s.getStartTime().getDayOfWeek(), soldTickets, Long::sum);
        }

        double occupancyRate = totalSeats == 0 ? 0.0 : totalTickets * 100.0 / totalSeats;

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

        int totalElements = showtimeRows.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        page = Math.max(1, page);
        page = totalPages > 0 ? Math.min(page, totalPages) : 1;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        List<ShowtimeAnalyticsRowResponse> pagedRows = showtimeRows.subList(start, end);

        return new ShowtimeAnalyticsResponse(
                startDate,
                endDate,
                dateError,
                sortedShowtimes.size(),
                totalTickets,
                totalSeats,
                occupancyRate,
                ticketRevenue,
                hourLabels,
                new ArrayList<>(ticketsByHour.values()),
                dayLabels,
                dayValues,
                new ArrayList<>(uniqueMovies.values()),
                pagedRows,
                page,
                totalPages,
                totalElements,
                pageSize
        );
    }

    private BigDecimal getShowtimeRevenue(Showtime showtime) {
        if (showtime.getTickets() == null) return BigDecimal.ZERO;
        return showtime.getTickets().stream()
                .filter(t -> t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.CHECKED_IN)
                .map(t -> t.getPrice() == null ? BigDecimal.ZERO : t.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
