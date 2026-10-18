
package fu.cinema.service.impl;

import fu.cinema.dto.response.*;
import fu.cinema.entity.*;
import fu.cinema.enums.PaymentStatus;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.BranchStockRepository;
import fu.cinema.repository.PaymentRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueReportServiceImpl implements RevenueReportService {
    private final BranchStockRepository branchStockRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeRepository showtimeRepository;
    @Override
    public RevenueReportResponse getReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            String revenueType
    ) {
        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc."
            );
        }

        LocalDateTime startTime = startDate == null
                ? null : startDate.atStartOfDay();

        LocalDateTime endTime = endDate == null
                ? null : endDate.plusDays(1).atStartOfDay();

        List<Payment> payments =
                paymentRepository.findSuccessfulPaymentsByBranchAndTime(
                        branchId,
                        PaymentStatus.SUCCESS,
                        startTime,
                        endTime
                );

        List<RevenueTransactionResponse> transactions = payments.stream()
                .map(payment -> {
                    BigDecimal ticketRevenue = payment.getBooking()
                            .getTickets()
                            .stream()
                            .filter(ticket ->
                                    ticket.getStatus()
                                            != fu.cinema.enums.TicketStatus.CANCELLED)
                            .map(Ticket::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal fnbRevenue = payment.getBooking()
                            .getBookingFnbs()
                            .stream()
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
                            (long) payment.getBooking().getTickets()
                                    .stream()
                                    .filter(ticket ->
                                            ticket.getStatus() == TicketStatus.BOOKED
                                                    || ticket.getStatus() == TicketStatus.CHECKED_IN)
                                    .count()

                    );
                })
                .filter(transaction -> {
                    if ("tickets".equalsIgnoreCase(revenueType)) {
                        return transaction.ticketRevenue()
                                .compareTo(BigDecimal.ZERO) > 0;
                    }

                    if ("fnb".equalsIgnoreCase(revenueType)) {
                        return transaction.fnbRevenue()
                                .compareTo(BigDecimal.ZERO) > 0;
                    }

                    return true;
                })
                .toList();

        BigDecimal ticketRevenue = transactions.stream()
                .map(RevenueTransactionResponse::ticketRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fnbRevenue = transactions.stream()
                .map(RevenueTransactionResponse::fnbRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue;

        if ("tickets".equalsIgnoreCase(revenueType)) {
            totalRevenue = ticketRevenue;
        } else if ("fnb".equalsIgnoreCase(revenueType)) {
            totalRevenue = fnbRevenue;
        } else {
            totalRevenue = transactions.stream()
                    .map(RevenueTransactionResponse::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return new RevenueReportResponse(
                totalRevenue,
                ticketRevenue,
                fnbRevenue,
                transactions.size(),
                transactions
        );
    }

    @Override
    public List<RevenueMovieResponse> getMovieReport(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate,
            MovieStatus movieStatus,
            String sortBy,
            String sortDir
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        List<Showtime> showtimes =
                showtimeRepository.findByHall_Branch_BranchIdOrderByStartTimeAsc(branchId)
                        .stream()
                        .filter(s -> startTime == null || !s.getStartTime().isBefore(startTime))
                        .filter(s -> endTime == null || s.getStartTime().isBefore(endTime))
                        .filter(s -> movieStatus == null || s.getMovie().getStatus() == movieStatus)
                        .toList();

        Map<Long, List<Showtime>> showtimesByMovie = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getMovie().getMovieId()));

        Set<Long> selectedShowtimeIds = showtimes.stream()
                .map(Showtime::getShowtimeId)
                .collect(Collectors.toSet());

        List<Payment> payments =
                paymentRepository.findSuccessfulPaymentsByBranchAndTime(
                        branchId, PaymentStatus.SUCCESS, startTime, endTime);

        Map<Long, BigDecimal> revenueByMovie = new HashMap<>();
        Map<Long, Long> ticketCountByMovie = new HashMap<>();

        for (Payment payment : payments) {
            if (payment.getBooking() == null || payment.getBooking().getTickets() == null) {
                continue;
            }

            for (Ticket ticket : payment.getBooking().getTickets()) {
                if (ticket.getShowtime() == null
                        || !selectedShowtimeIds.contains(ticket.getShowtime().getShowtimeId())) {
                    continue;
                }

                if (ticket.getStatus() == TicketStatus.BOOKED
                        || ticket.getStatus() == TicketStatus.CHECKED_IN) {
                    Long movieId = ticket.getShowtime().getMovie().getMovieId();

                    revenueByMovie.merge(movieId, ticket.getPrice(), BigDecimal::add);
                    ticketCountByMovie.merge(movieId, 1L, Long::sum);
                }
            }
        }

        Comparator<RevenueMovieResponse> comparator;
        switch (sortBy == null ? "title" : sortBy.toLowerCase()) {
            case "revenue" -> comparator = Comparator.comparing(RevenueMovieResponse::revenue);
            case "showtimes" -> comparator = Comparator.comparingLong(RevenueMovieResponse::showtimeCount);
            default -> comparator = Comparator.comparing(
                    RevenueMovieResponse::movieTitle,
                    String.CASE_INSENSITIVE_ORDER
            );
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return showtimesByMovie.entrySet().stream()
                .map(entry -> {
                    Long movieId = entry.getKey();
                    List<Showtime> movieShowtimes = entry.getValue();
                    Showtime firstShowtime = movieShowtimes.get(0);

                    long totalSeats = movieShowtimes.stream()
                            .mapToLong(showtime -> showtime.getHall().getSeats().size())
                            .sum();

                    return new RevenueMovieResponse(
                            movieId,
                            firstShowtime.getMovie().getTitle(),
                            firstShowtime.getMovie().getPosterUrl(),
                            firstShowtime.getMovie().getStatus(),
                            movieShowtimes.size(),
                            ticketCountByMovie.getOrDefault(movieId, 0L),
                            totalSeats,
                            revenueByMovie.getOrDefault(movieId, BigDecimal.ZERO)
                    );
                })
                .sorted(comparator)
                .toList();
    }

    @Override
    public List<RevenueShowtimeResponse> getShowtimeReport(
            Long branchId,
            Long movieId,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir
    ) {

        LocalDateTime startTime = startDate == null
                ? null : startDate.atStartOfDay();

        LocalDateTime endTime = endDate == null
                ? null : endDate.plusDays(1).atStartOfDay();

        List<Showtime> showtimes =
                showtimeRepository.findByHall_Branch_BranchIdOrderByStartTimeAsc(
                                branchId
                        ).stream()
                        .filter(s -> s.getMovie().getMovieId().equals(movieId))
                        .filter(s -> startTime == null
                                || !s.getStartTime().isBefore(startTime))
                        .filter(s -> endTime == null
                                || s.getStartTime().isBefore(endTime))
                        .toList();

        List<Payment> payments =
                paymentRepository.findSuccessfulPaymentsByBranchAndTime(
                        branchId,
                        PaymentStatus.SUCCESS,
                        startTime,
                        endTime
                );

        Map<Long, BigDecimal> revenueByShowtime = new java.util.HashMap<>();
        Map<Long, Long> ticketCountByShowtime = new java.util.HashMap<>();

        for (Payment payment : payments) {
            for (Ticket ticket : payment.getBooking().getTickets()) {
                if ((ticket.getStatus() == TicketStatus.BOOKED
                        || ticket.getStatus() == TicketStatus.CHECKED_IN)
                        && ticket.getShowtime().getMovie().getMovieId()
                        .equals(movieId)) {

                    Long showtimeId = ticket.getShowtime().getShowtimeId();

                    revenueByShowtime.merge(
                            showtimeId, ticket.getPrice(), BigDecimal::add
                    );

                    ticketCountByShowtime.merge(
                            showtimeId, 1L, Long::sum
                    );
                }
            }
        }

        return showtimes.stream()
                .map(showtime -> {
                    Long id = showtime.getShowtimeId();
                    long totalSeats = showtime.getHall().getSeats().size();
                    return new RevenueShowtimeResponse(
                            id,
                            showtime.getMovie().getTitle(),
                            showtime.getHall().getHallName(),
                            showtime.getStartTime(),
                            showtime.getEndTime(),
                            ticketCountByShowtime.getOrDefault(id, 0L),
                            revenueByShowtime.getOrDefault(id, BigDecimal.ZERO),
                            totalSeats

                    );
                })
                .sorted((a, b) -> {
            int result;

            if ("revenue".equalsIgnoreCase(sortBy)) {
                result = a.revenue().compareTo(b.revenue());
            } else {
                result = a.startTime().compareTo(b.startTime());
            }

            return "desc".equalsIgnoreCase(sortDir)
                    ? -result
                    : result;
        })
                .toList();
    }
    @Override
    @Transactional(readOnly = true)

    public RevenueReportResponse getShowtimeDetail(
            Long branchId,
            Long showtimeId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod
    ) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy suất chiếu."));

        // Không cho Manager xem báo cáo của chi nhánh khác
        if (showtime.getHall() == null
                || showtime.getHall().getBranch() == null
                || !showtime.getHall().getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException(
                    "Bạn không có quyền xem báo cáo của suất chiếu này."
            );
        }

        String method = paymentMethod == null
                ? "all"
                : paymentMethod.trim().toUpperCase();

        if (!List.of("ALL", "CASH", "ONLINE_QR", "E_WALLET")
                .contains(method)) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ."
            );
        }

        LocalDateTime from = startDate == null
                ? null
                : startDate.atStartOfDay();

        LocalDateTime toExclusive = endDate == null
                ? null
                : endDate.plusDays(1).atStartOfDay();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc."
            );
        }

        BigDecimal ticketRevenue = BigDecimal.ZERO;
        BigDecimal fnbRevenue = BigDecimal.ZERO;
        long ticketCount = 0;

        List<RevenueTransactionResponse> transactions = new ArrayList<>();

        Set<Long> processedBookingIds = new HashSet<>();

        for (Payment payment : paymentRepository.findAll()) {
            if (payment.getStatus() != PaymentStatus.SUCCESS
                    || payment.getBooking() == null) {
                continue;
            }
            if (!"ALL".equals(method)
                    && (payment.getPaymentMethod() == null
                    || !method.equals(payment.getPaymentMethod().name()))) {
                continue;
            }

            LocalDateTime transactionTime = payment.getTransactionTime();

            if (transactionTime == null
                    || (from != null && transactionTime.isBefore(from))
                    || (toExclusive != null
                    && !transactionTime.isBefore(toExclusive))) {
                continue;
            }

            Booking booking = payment.getBooking();

            // Chỉ lấy vé của đúng suất chiếu đang xem
            List<Ticket> matchingTickets = booking.getTickets().stream()
                    .filter(ticket -> ticket.getShowtime() != null)
                    .filter(ticket -> showtimeId.equals(
                            ticket.getShowtime().getShowtimeId()))
                    .filter(ticket ->
                            ticket.getStatus() == TicketStatus.BOOKED
                                    || ticket.getStatus() == TicketStatus.CHECKED_IN)
                    .toList();

            if (matchingTickets.isEmpty()) {
                continue;
            }

            if (!processedBookingIds.add(booking.getBookingId())) {
                continue;
            }

            boolean bookingBelongsToShowtime = !matchingTickets.isEmpty();

            // F&B chỉ được tính khi xác định được booking thuộc suất chiếu
            // tại chi nhánh này; không gán F&B không rõ nguồn cho chi nhánh.
            if (!bookingBelongsToShowtime) {
                continue;
            }


            BigDecimal ticketAmount = matchingTickets.stream()
                    .map(Ticket::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal fnbAmount = booking.getBookingFnbs().stream()
                    .map(BookingFnB::getSubtotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ticketRevenue = ticketRevenue.add(ticketAmount);
            fnbRevenue = fnbRevenue.add(fnbAmount);
            ticketCount += matchingTickets.size();

            BigDecimal transactionAmount = ticketAmount.add(fnbAmount);

            if (transactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                transactions.add(new RevenueTransactionResponse(
                        payment.getPaymentId(),
                        booking.getBookingId(),
                        transactionTime,
                        payment.getPaymentMethod() == null
                                ? "N/A"
                                : payment.getPaymentMethod().name(),
                        payment.getAmount() != null
                                ? payment.getAmount()
                                : transactionAmount,
                        ticketAmount,
                        fnbAmount,
                        matchingTickets.size()
                ));

            }
        }




        // Hiển thị giao dịch mới nhất trước
        transactions.sort((a, b) ->
                b.transactionTime().compareTo(a.transactionTime()));

        BigDecimal totalRevenue = ticketRevenue.add(fnbRevenue);
        System.out.println("FINAL ticketRevenue = " + ticketRevenue);
        System.out.println("FINAL fnbRevenue = " + fnbRevenue);
        System.out.println("FINAL totalRevenue = " + totalRevenue);
        System.out.println("FINAL transactionCount = " + transactions.size());
        return new RevenueReportResponse(
                ticketRevenue,
                ticketRevenue,
                BigDecimal.ZERO,
                transactions.size(),
                transactions
        );
    }
    @Override
    public List<FnbRevenueRowResponse> getFnbRevenueRows(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc."
            );
        }

        LocalDateTime startTime = startDate == null
                ? null : startDate.atStartOfDay();

        LocalDateTime endTime = endDate == null
                ? null : endDate.plusDays(1).atStartOfDay();

        return paymentRepository.findSuccessfulPaymentsByBranchAndTime(
                        branchId,
                        PaymentStatus.SUCCESS,
                        startTime,
                        endTime
                )
                .stream()
                .filter(payment -> payment.getBooking() != null)
                .flatMap(payment -> payment.getBooking()
                        .getBookingFnbs()
                        .stream()
                        .map(item -> new FnbRevenueRowResponse(
                                item.getFnbItem().getItemId(),
                                payment.getPaymentId(),
                                payment.getTransactionTime(),
                                payment.getPaymentMethod() == null
                                        ? "N/A"
                                        : payment.getPaymentMethod().name(),
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
        List<FnbRevenueRowResponse> rows =
                getFnbRevenueRows(branchId, startDate, endDate);

        Map<Long, List<FnbRevenueRowResponse>> rowsByItem =
                rows.stream().collect(Collectors.groupingBy(
                        FnbRevenueRowResponse::itemId
                ));

        Comparator<FnbProductRevenueResponse> comparator;

        switch (sortBy == null ? "name" : sortBy) {
            case "sold" ->
                    comparator = Comparator.comparingLong(
                            FnbProductRevenueResponse::soldQuantity);
            case "revenue" ->
                    comparator = Comparator.comparing(
                            FnbProductRevenueResponse::revenue);
            default ->
                    comparator = Comparator.comparing(
                            FnbProductRevenueResponse::itemName,
                            String.CASE_INSENSITIVE_ORDER);
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return branchStockRepository.findByBranchId(branchId)
                .stream()
                .map(stock -> {
                    FnBItem item = stock.getItem();

                    List<FnbRevenueRowResponse> itemRows =
                            rowsByItem.getOrDefault(
                                    item.getItemId(), Collections.emptyList());

                    long sold = itemRows.stream()
                            .mapToLong(row -> row.quantity() == null
                                    ? 0 : row.quantity())
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
                .filter(item -> "ALL".equalsIgnoreCase(category)
                        || item.category().equalsIgnoreCase(category))
                .sorted(comparator)
                .toList();
    }

    @Override
    public List<FnbRevenueRowResponse> getFnbItemTransactions(
            Long branchId,
            Long itemId,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String sortDir
    ) {
        boolean itemExists = branchStockRepository
                .findByBranchIdAndItemId(branchId, itemId)
                .isPresent();

        if (!itemExists) {
            throw new IllegalArgumentException(
                    "Món F&B không tồn tại trong kho chi nhánh này.");
        }

        String method = paymentMethod == null
                ? "ALL" : paymentMethod.toUpperCase();

        Comparator<FnbRevenueRowResponse> comparator =
                Comparator.comparing(
                        FnbRevenueRowResponse::transactionTime,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        if (!"asc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return getFnbRevenueRows(branchId, startDate, endDate)
                .stream()
                .filter(row -> row.itemId().equals(itemId))
                .filter(row -> "ALL".equals(method)
                        || method.equals(row.paymentMethod()))
                .sorted(comparator)
                .toList();
    }

}
