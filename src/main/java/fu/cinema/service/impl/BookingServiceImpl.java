package fu.cinema.service.impl;

import fu.cinema.dto.request.SeatLockRequest;
import fu.cinema.dto.response.*;
import fu.cinema.entity.*;
import fu.cinema.enums.*;
import fu.cinema.exception.*;
import fu.cinema.repository.*;
import fu.cinema.service.BookingService;
import fu.cinema.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl implements BookingService {
    TicketRepository ticketRepository;
    SeatRepository seatRepository;
    ShowtimeRepository showtimeRepository;
    BookingRepository bookingRepository;
    AccountRepository accountRepository;
    PromotionRepository promotionRepository;
    BookingFnBRepository bookingFnbRepository;
    PaymentRepository paymentRepository;
    BranchStockRepository branchStockRepository;
    NotificationService notificationService;

    private BigDecimal calculateSeatPrice(SeatType seatType, BigDecimal basePrice){
        if(seatType == SeatType.VIP){
            return basePrice.add(BigDecimal.valueOf(10000));
        }
        if(seatType == SeatType.SWEETBOX){
            return basePrice.add(BigDecimal.valueOf(20000));
        }
        return basePrice;
    }

    // Hàm lấy danh sách ghế của Hall trong 1 showtime (bao gồm các ghế đã bị khóa, giá tiền của từng ghế)
    @Override
    @Transactional(readOnly = true)
    public SeatSelectionResponse getSeatSelectionData(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElseThrow(ShowtimeNotFoundException::new);

        // Lấy danh sách seatIds của các ghế đã được Booked/Locked/CheckedIn
        List<TicketStatus> statuses = List.of(TicketStatus.BOOKED, TicketStatus.LOCKED, TicketStatus.CHECKED_IN);
        List<Long> bookedSeatIds = ticketRepository.findBookedSeatIdsByShowtimeId(showtimeId, statuses);

        // Lấy tất cả các ghế của hall đó
        Long hallId = showtime.getHall().getHallId();
        List<Seat> allSeatsInHall = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hallId);

        // Gom ghế theo hàng (Key là hàng A,B,C,... còn Value là danh sách ghế của các hàng đó)
        Map<String, List<BookingSeatResponse>> seatMap = new LinkedHashMap<>();

        // Vòng lặp đưa ghế vào các hàng
        for(Seat seat : allSeatsInHall){
            String row = seat.getRowCode();

            if(!seatMap.containsKey(row)){
                seatMap.put(row, new ArrayList<>());
            }

            boolean isBooked = bookedSeatIds.contains(seat.getSeatId());
            boolean isBroken = seat.getStatus() == SeatStatus.BROKEN;

            BigDecimal price = calculateSeatPrice(seat.getType(), showtime.getBasePrice());

            BookingSeatResponse bookingSeatResponse = BookingSeatResponse.builder()
                    .seatId(seat.getSeatId())
                    .rowCode(seat.getRowCode())
                    .number(seat.getNumber())
                    .seatName(seat.getRowCode() + seat.getNumber())
                    .type(seat.getType())
                    .price(price)
                    .isBooked(isBooked)
                    .isBroken(isBroken)
                    .build();

            seatMap.get(row).add(bookingSeatResponse);
        }

        // Trả về thông tin cần thiết cho giao diện người dùng
        return SeatSelectionResponse.builder()
                .showtimeId(showtime.getShowtimeId())
                .movieTitle(showtime.getMovie().getTitle())
                .posterUrl(showtime.getMovie().getPosterUrl())
                .ageRating(showtime.getMovie().getAgeRating())
                .branchName(showtime.getHall().getBranch().getBranchName())
                .hallName(showtime.getHall().getHallName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .seatMap(seatMap)
                .build();
    }

    // Hàm xử lí các request khóa ghế mà người dùng chọn
    @Override
    @Transactional
    public Long lockSeats(SeatLockRequest request) {
        if(request.getSeatIds() == null || request.getSeatIds().isEmpty()){
            throw new AppException("Vui lòng chọn ít nhất một ghế!");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account currentAccount = accountRepository.findByUsername(auth.getName()).orElseThrow(AccountNotFoundException::new);
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId()).orElseThrow(ShowtimeNotFoundException::new);

        // Tìm những ghế đã bị khóa
        List<TicketStatus> statuses = List.of(TicketStatus.BOOKED, TicketStatus.LOCKED, TicketStatus.CHECKED_IN);
        List<Long> unavailableSeatIds = ticketRepository.findBookedSeatIdsByShowtimeId(request.getShowtimeId(), statuses);

        // Kiểm tra xem ghế đó đã có người nào chọn chưa, tránh 1 ghế có 2 vé trong cùng 1 showtime
        for(Long seatId : request.getSeatIds()){
            if(unavailableSeatIds.contains(seatId)){
                throw new AppException("Ghế bạn chọn đã có người khác giữ trước hoặc đang không khả dụng. Vui lòng chọn ghế khác!");
            }
        }

        List<Seat> seatsToLock = seatRepository.findAllById(request.getSeatIds());

        // BẮT ĐẦU RÀNG BUỘC SWEETBOX
        List<Seat> sweetboxSeats = seatsToLock.stream()
                .filter(seat -> seat.getType() == SeatType.SWEETBOX)
                .toList();

        if (!sweetboxSeats.isEmpty()) {
            if (sweetboxSeats.size() % 2 != 0) {
                throw new AppException("Ghế đôi (Sweetbox) bắt buộc phải mua theo cặp. Bạn đang chọn lẻ ghế, vui lòng chọn lại!");
            }

            // Kéo toàn bộ ghế của rạp ra để đối chiếu danh sách cặp gốc
            Long hallId = showtime.getHall().getHallId();
            List<Seat> allSeatsInHall = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hallId);

            Map<String, List<Seat>> sweetboxByRow = sweetboxSeats.stream()
                    .collect(Collectors.groupingBy(Seat::getRowCode));

            for (Map.Entry<String, List<Seat>> entry : sweetboxByRow.entrySet()) {
                String rowCode = entry.getKey();
                List<Seat> selectedRowSeats = entry.getValue();

                if (selectedRowSeats.size() % 2 != 0) {
                    throw new AppException("Ghế đôi tại hàng " + rowCode + " đang chọn lẻ. Vui lòng chọn đủ cặp!");
                }

                // 1. Lọc ra toàn bộ ghế Sweetbox của hàng này trong rạp
                List<Seat> allSweetboxesInRow = allSeatsInHall.stream()
                        .filter(s -> s.getRowCode().equals(rowCode) && s.getType() == SeatType.SWEETBOX)
                        .sorted(Comparator.comparing(Seat::getNumber))
                        .toList();

                // 2. Chia cặp "chuẩn" dựa trên index chẵn-lẻ gốc của sơ đồ
                List<String> validPairs = new ArrayList<>();
                for (int i = 0; i < allSweetboxesInRow.size() - 1; i += 2) {
                    validPairs.add(allSweetboxesInRow.get(i).getSeatId() + "-" + allSweetboxesInRow.get(i + 1).getSeatId());
                }

                // 3. Đối chiếu xem khách có chọn đúng các cặp đã đóng đinh ở trên không
                selectedRowSeats.sort(Comparator.comparing(Seat::getNumber));
                for (int i = 0; i < selectedRowSeats.size(); i += 2) {
                    String selectedPair = selectedRowSeats.get(i).getSeatId() + "-" + selectedRowSeats.get(i + 1).getSeatId();

                    if (!validPairs.contains(selectedPair)) {
                        throw new AppException("Bạn đã chọn sai cặp ghế đôi ở hàng " + rowCode + " (chọn chéo 2 ghế của 2 cặp khác nhau). Vui lòng nhìn sơ đồ và chọn đúng cặp!");
                    }
                }
            }
        }
        // KẾT THÚC RÀNG BUỘC SWEETBOX NÂNG CẤP

        // Tính tiền cho mỗi ghế (giá base + giá tùy loại ghế)
        BigDecimal totalAmount = BigDecimal.ZERO;
        for(Seat seat : seatsToLock){
            totalAmount = totalAmount.add(calculateSeatPrice(seat.getType(), showtime.getBasePrice()));
        }

        Booking booking = Booking.builder()
                .account(currentAccount)
                .bookingTime(LocalDateTime.now())
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Mỗi vé tương ứng 1 ghế trong 1 showtime
        for(Seat seat : seatsToLock){
            Ticket ticket = Ticket.builder()
                    .booking(savedBooking)
                    .seat(seat)
                    .showtime(showtime)
                    .price(calculateSeatPrice(seat.getType(), showtime.getBasePrice()))
                    .status(TicketStatus.LOCKED)
                    .qrCode("TEMP_"+ UUID.randomUUID())
                    .build();
            ticketRepository.save(ticket);
        }
        return savedBooking.getBookingId();
    }

    // Hàm chạy ngầm mỗi 60s (Check xem booking nào đang ở trạng thái PENDING và được tạo trước thời điểm hiện tại - 10p)
    @Override
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredSeats() {
        LocalDateTime tenMinsAgo = LocalDateTime.now().minusMinutes(10);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, tenMinsAgo);

        if(!expiredBookings.isEmpty()){
            for(Booking booking : expiredBookings){
                // XÓA CON TRƯỚC - CHA SAU ĐỂ TRÁNH LỖI KHÓA NGOẠI
                if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
                    ticketRepository.deleteAll(booking.getTickets());
                }

                List<BookingFnB> fnbList = bookingFnbRepository.findByBooking_BookingId(booking.getBookingId());
                if (fnbList != null && !fnbList.isEmpty()) {
                    bookingFnbRepository.deleteAll(fnbList);
                }

                paymentRepository.findByBooking_BookingId(booking.getBookingId()).ifPresent(paymentRepository::delete);

                bookingRepository.delete(booking);
            }
        }
    }

    // Hàm trả về thông tin người dùng đặt vé (thông tin vé, tổng giá tiền,...)
    @Override
    @Transactional(readOnly = true)
    public ReviewOrderResponse getReviewOrderData(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);

        if(booking.getStatus() != BookingStatus.PENDING){
            throw new AppException("Đơn hàng không hợp lệ hoặc đã hoàn tất.");
        }

        // Kiểm tra xem đây là đơn Kèm Vé hay Đơn Bán Lẻ
        boolean isRetail = booking.getTickets() == null || booking.getTickets().isEmpty();

        String movieTitle = "---";
        String posterUrl = "/images/default-fnb.png"; // Hiển thị ảnh ly bắp nước mặc định
        String branchName = "---";
        String hallName = "Mua Tại Quầy (Mang Về)";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now();
        List<String> seatNames = new ArrayList<>();
        BigDecimal ticketTotal = BigDecimal.ZERO;

        if (!isRetail) {
            // NẾU CÓ VÉ -> Lấy thông tin phim bình thường
            Showtime showtime = booking.getTickets().get(0).getShowtime();
            movieTitle = showtime.getMovie().getTitle();
            posterUrl = showtime.getMovie().getPosterUrl() != null ? showtime.getMovie().getPosterUrl() : "/images/posters/default-poster.jpg";
            branchName = showtime.getHall().getBranch().getBranchName();
            hallName = showtime.getHall().getHallName();
            startTime = showtime.getStartTime();
            endTime = showtime.getEndTime();

            // Tính tổng tiền vé
            for(Ticket ticket : booking.getTickets()){
                seatNames.add(ticket.getSeat().getRowCode() + ticket.getSeat().getNumber());
                ticketTotal = ticketTotal.add(ticket.getPrice());
            }
        } else {
            // NẾU LÀ BÁN LẺ -> Chèn dữ liệu giả để Review Order không bị sập
            movieTitle = "Bán Lẻ Bắp Nước (Không kèm vé)";
            if (booking.getAccount() != null && booking.getAccount().getStaff() != null) {
                branchName = booking.getAccount().getStaff().getBranch().getBranchName();
            }
        }

        // Giá tiền F&B
        List<String> fnbDetails = new ArrayList<>();
        BigDecimal fnbTotal = BigDecimal.ZERO;
        if(booking.getBookingFnbs() != null && !booking.getBookingFnbs().isEmpty()){
            for(BookingFnB bookingFnB : booking.getBookingFnbs()){
                fnbDetails.add(bookingFnB.getQuantity() + "x " + bookingFnB.getFnbItem().getName() + " (" + bookingFnB.getSubtotal() + "đ)");
                fnbTotal = fnbTotal.add(bookingFnB.getSubtotal());
            }
        }

        BigDecimal discountAmount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;

        // Tổng giá tiền cuối cùng
        BigDecimal finalAmount = ticketTotal.add(fnbTotal).subtract(discountAmount);
        if(finalAmount.compareTo(BigDecimal.ZERO) < 0){
            finalAmount = BigDecimal.ZERO;
        }

        return ReviewOrderResponse.builder()
                .bookingId(booking.getBookingId())
                .movieTitle(movieTitle)
                .posterUrl(posterUrl)
                .branchName(branchName)
                .hallName(hallName)
                .startTime(startTime)
                .endTime(endTime)
                .seatNames(seatNames)
                .ticketTotal(ticketTotal)
                .fnbDetails(fnbDetails)
                .fnbTotal(fnbTotal)
                .appliedPromoCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    // Hàm áp dụng mã khuyến mãi (promotion)
    @Override
    @Transactional
    public void applyPromotion(Long bookingId, String promoCode) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);
        Promotion promotion = promotionRepository.findByCode(promoCode).orElseThrow(PromoNotFoundException::new);

        if(promotion.getStatus()!= PromotionStatus.ACTIVE){
            throw new AppException("Mã khuyến mãi này không còn khả dụng.");
        }
        if(promotion.getValidFrom()!=null && promotion.getValidFrom().isAfter(LocalDateTime.now())){
            throw new AppException("Mã khuyến mãi này chưa đến ngày bắt đầu sử dụng.");
        }
        if(promotion.getExpiryDate()!=null && promotion.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new AppException(("Mã khuyến mãi này đã hết hạn sử dụng."));
        }

        // =================================================================
        // CHỐT CHẶN THÉP: KIỂM TRA LỊCH SỬ DÙNG MÃ CỦA USER
        // =================================================================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if (username != null && !username.equals("anonymousUser")) {
            Account currentUser = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException("Không tìm thấy thông tin tài khoản!"));

            // Kiểm tra xem người dùng hiện tại có phải là Nhân viên/Quản lý không
            boolean isStaffOrManager = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_MANAGER"));

            // CHỈ KHÁCH HÀNG THÌ MỚI KIỂM TRA ĐỂ TRÁNH NHÂN VIÊN POS BỊ CHẶN KHI ÁP MÃ CHO NHIỀU KHÁCH
            if (!isStaffOrManager) {
                // Bỏ qua các đơn bị hủy (CANCELLED) và bỏ qua chính đơn hàng này (bookingId)
                boolean isAlreadyUsed = bookingRepository.existsByAccount_AccountIdAndPromotion_IdAndStatusNotAndBookingIdNot(
                        currentUser.getAccountId(),
                        promotion.getId(),
                        BookingStatus.CANCELLED,
                        bookingId
                );

                if (isAlreadyUsed) {
                    throw new AppException("Tài khoản của bạn đã sử dụng mã giảm giá này cho một đơn hàng khác!");
                }
            }
        } else {
            throw new AppException("Vui lòng đăng nhập để sử dụng mã khuyến mãi!");
        }
        // =================================================================

        // TÍNH LẠI CHÍNH XÁC TỔNG TIỀN VÉ GỐC
        BigDecimal ticketTotal = BigDecimal.ZERO;
        if (booking.getTickets() != null) {
            for (Ticket ticket : booking.getTickets()) {
                ticketTotal = ticketTotal.add(ticket.getPrice());
            }
        }

        // TÍNH LẠI CHÍNH XÁC TỔNG TIỀN BẮP NƯỚC
        BigDecimal fnbTotal = BigDecimal.ZERO;
        if (booking.getBookingFnbs() != null && !booking.getBookingFnbs().isEmpty()) {
            for (BookingFnB fnB : booking.getBookingFnbs()) {
                fnbTotal = fnbTotal.add(fnB.getSubtotal());
            }
        }

        // TỔNG TIỀN GỐC (CHƯA GIẢM GIÁ)
        BigDecimal totalAmountWithoutDiscount = ticketTotal.add(fnbTotal);

        // TÍNH TOÁN SỐ TIỀN ĐƯỢC GIẢM
        BigDecimal discount = BigDecimal.ZERO;

        if ("FIXED_AMOUNT".equalsIgnoreCase(promotion.getDiscountType())) {
            discount = BigDecimal.valueOf(promotion.getDiscountPercentage());
            if (discount.compareTo(totalAmountWithoutDiscount) > 0) {
                discount = totalAmountWithoutDiscount;
            }
        } else {
            discount = totalAmountWithoutDiscount
                    .multiply(BigDecimal.valueOf(promotion.getDiscountPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (promotion.getMaxDiscountAmount() != null
                    && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                    && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
        }

        // CHỈ LƯU MÃ VÀ SỐ TIỀN GIẢM VÀO DB (Không set cứng TotalAmount ở đây)
        booking.setPromotion(promotion);
        booking.setDiscountAmount(discount);

        // Cập nhật lại luôn TotalAmount chuẩn xác để tránh lỗi đồng bộ
        BigDecimal finalTotal = totalAmountWithoutDiscount.subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }
        booking.setTotalAmount(finalTotal);
        bookingRepository.save(booking);
    }

    // Hàm chạy khi người dùng bấm hủy đơn (giải phóng ghế) + Gửi thông báo FE-10.1
    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn đặt vé"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException("Chỉ có thể hủy đơn hàng đang chờ thanh toán.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            ticketRepository.deleteAll(booking.getTickets());
        }

        List<BookingFnB> fnbList = bookingFnbRepository.findByBooking_BookingId(bookingId);
        if (fnbList != null && !fnbList.isEmpty()) {
            bookingFnbRepository.deleteAll(fnbList);
        }

        // FE-10.1: Gửi thông báo hủy đơn đặt vé
        notificationService.sendBookingCancelledAlert(booking);

        paymentRepository.findByBooking_BookingId(bookingId).ifPresent(paymentRepository::delete);
        bookingRepository.delete(booking);
    }

    // Danh sách bookings (Staff)
    @Override
    @Transactional(readOnly = true)
    public List<BookDetailsResponse> getAllBookings(String keyword, BookingStatus status) {
        List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<BookDetailsResponse> result = new ArrayList<>();

        for (Booking b : allBookings) {
            // Lọc theo trạng thái nếu có chọn
            if (status != null && b.getStatus() != status) {
                continue;
            }

            boolean isRetail = (b.getTickets() == null || b.getTickets().isEmpty());
            boolean isEmptyFnb = (b.getBookingFnbs() == null || b.getBookingFnbs().isEmpty());
            if (b.getStatus() == BookingStatus.PENDING && isRetail && isEmptyFnb) {
                continue;
            }
            String customerName = "---";
            String customerPhone = "---";
            if (b.getAccount() != null) {
                customerName = b.getAccount().getUsername();
                if (b.getAccount().getCustomer() != null) {
                    customerName = b.getAccount().getCustomer().getFullName();
                    customerPhone = b.getAccount().getCustomer().getPhone();
                }
            }

            // Lấy tên phim
            String movieTitle = (b.getTickets() != null && !b.getTickets().isEmpty()) ? b.getTickets().get(0).getShowtime().getMovie().getTitle() : "---";

            // Tìm kiếm theo từ khóa (Mã đơn, Tên khách, SĐT hoặc Tên phim)
            if (keyword != null && !keyword.trim().isEmpty()) {
                keyword = keyword.trim().toLowerCase();
                boolean matchId = String.valueOf(b.getBookingId()).contains(keyword);
                boolean matchCustomer = customerName.toLowerCase().contains(keyword) || customerPhone.contains(keyword);
                boolean matchMovie = movieTitle.toLowerCase().contains(keyword);

                // Nếu không khớp bất kỳ thông tin nào -> bỏ qua
                if (!matchId && !matchCustomer && !matchMovie) {
                    continue;
                }
            }

            // Đóng gói, gửi về controller
            result.add(BookDetailsResponse.builder()
                    .bookingId(b.getBookingId())
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .movieTitle(movieTitle)
                    .totalAmount(b.getTotalAmount())
                    .status(b.getStatus())
                    .build());
        }
        return result;
    }

    // Lấy chi tiết booking (Dùng cho staff)
    @Override
    @Transactional(readOnly = true)
    public BookDetailsResponse getBookingDetail(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);

        String name = b.getAccount() != null ? b.getAccount().getUsername() : "Khách vãng lai";
        String phone = "---";
        if (b.getAccount() != null && b.getAccount().getCustomer() != null) {
            name = b.getAccount().getCustomer().getFullName();
            phone = b.getAccount().getCustomer().getPhone();
        }
        String movie = (b.getTickets() != null && !b.getTickets().isEmpty()) ? b.getTickets().get(0).getShowtime().getMovie().getTitle() : "---";
        String hall = (b.getTickets() != null && !b.getTickets().isEmpty()) ? b.getTickets().get(0).getShowtime().getHall().getHallName() : "---";
        LocalDateTime time = (b.getTickets() != null && !b.getTickets().isEmpty()) ? b.getTickets().get(0).getShowtime().getStartTime() : null;
        List<String> seats = new ArrayList<>();
        if (b.getTickets() != null) {
            for (Ticket t : b.getTickets()) {
                seats.add(t.getSeat().getRowCode() + t.getSeat().getNumber());
            }
        }
        return BookDetailsResponse.builder()
                .bookingId(b.getBookingId())
                .customerName(name)
                .customerPhone(phone)
                .movieTitle(movie)
                .hallName(hall)
                .startTime(time)
                .seatNames(String.join(", ", seats))
                .totalAmount(b.getTotalAmount())
                .status(b.getStatus())
                .build();
    }

    // --- XÓA CỨNG ĐƠN HÀNG RÁC (PENDING) DÀNH CHO STAFF ---
    @Override
    @Transactional
    public void hardDeletePendingBooking(Long bookingId, Long staffBranchId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn hàng"));

        Long bookingBranchId = (booking.getTickets() == null || booking.getTickets().isEmpty())
                ? staffBranchId
                : booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId();

        if (!bookingBranchId.equals(staffBranchId)) {
            throw new AppException("Đơn hàng này không thuộc chi nhánh của bạn!");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException("Chỉ được phép xóa cứng các đơn hàng rác (Chờ thanh toán).");
        }

        // Xóa thủ công dữ liệu con để tránh lỗi khóa ngoại
        if (booking.getTickets() != null) {
            ticketRepository.deleteAll(booking.getTickets());
        }

        List<BookingFnB> fnbList = bookingFnbRepository.findByBooking_BookingId(bookingId);
        if (fnbList != null && !fnbList.isEmpty()) {
            bookingFnbRepository.deleteAll(fnbList);
        }

        paymentRepository.findByBooking_BookingId(bookingId).ifPresent(paymentRepository::delete);
        bookingRepository.delete(booking);
    }

    // --- HỦY VÀ HOÀN TIỀN ĐƠN HÀNG HỢP LỆ (CONFIRMED) ---
    @Override
    @Transactional
    public void cancelConfirmedBooking(Long bookingId, Long staffBranchId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn hàng"));

        Long bookingBranchId = (booking.getTickets() == null || booking.getTickets().isEmpty())
                ? staffBranchId
                : booking.getTickets().get(0).getShowtime().getHall().getBranch().getBranchId();

        if (!bookingBranchId.equals(staffBranchId)) {
            throw new AppException("Đơn hàng này không thuộc chi nhánh của bạn!");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException("Chỉ có thể hủy đơn đã thanh toán thành công!");
        }

        boolean isUsed = booking.getTickets().stream().anyMatch(t -> t.getStatus() == TicketStatus.CHECKED_IN);
        if (isUsed) {
            throw new AppException("Đơn hàng này đã có vé được soát. Tuyệt đối không thể hủy!");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getTickets() != null) {
            booking.getTickets().forEach(t -> t.setStatus(TicketStatus.CANCELLED));
        }

        // Hoàn bắp nước lại kho
        List<BookingFnB> fnbList = bookingFnbRepository.findByBooking_BookingId(bookingId);
        if (fnbList != null && !fnbList.isEmpty()) {
            for (BookingFnB fnb : fnbList) {
                branchStockRepository.findByBranch_BranchIdAndItem_ItemId(bookingBranchId, fnb.getFnbItem().getItemId())
                        .ifPresent(stock -> {
                            stock.setQuantity(stock.getQuantity() + fnb.getQuantity());
                            branchStockRepository.save(stock);
                        });
            }
        }
        Booking saved = bookingRepository.save(booking);

        notificationService.sendBookingCancelledAlert(saved);
    }

    // Lấy lịch sử mua vé của người dùng
    @Override
    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getMyBookings(String keyword, BookingStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account currentUser = accountRepository.findByUsername(auth.getName()).orElseThrow(AccountNotFoundException::new);

        List<Booking> bookings = bookingRepository.findByAccount_AccountIdOrderByCreatedAtDesc(currentUser.getAccountId());

        List<BookingHistoryResponse> responses = new ArrayList<>();

        for(Booking b : bookings){

            // Nếu lọc theo Status mà status của booking này khác với status muốn lọc thì continue
            if(status!=null && b.getStatus()!=status){
                continue;
            }

            List<Ticket> tickets = b.getTickets();
            String movieTitle = "---";
            LocalDateTime showtime = null;
            List<String> seatList = new ArrayList<>();

            // Check xem tickets của booking có null hay empty không
            if(tickets!=null && !tickets.isEmpty()){
                movieTitle = b.getTickets().get(0).getShowtime().getMovie().getTitle();
                showtime = b.getTickets().get(0).getShowtime().getStartTime();
                // Duyệt qua ticket để lấy tên các ghế (seatRow + seatNumber)
                for(Ticket ticket : b.getTickets()){
                    seatList.add(ticket.getSeat().getRowCode()+ticket.getSeat().getNumber());
                }
            }

            // Nếu người dùng nhập vào ô tìm kiếm -> có keyword
            if(keyword!=null && !keyword.trim().isEmpty()){
                keyword = keyword.trim().toLowerCase();
                boolean isMatchMovieTitle = movieTitle.toLowerCase().contains(keyword);
                boolean isMatchBookingId = String.valueOf(b.getBookingId()).contains(keyword);

                // Nếu keyword không contains cả movieTitle và bookingId -> chuyển tới phần tử kế
                if(!isMatchMovieTitle && !isMatchBookingId){
                    continue;
                }
            }

            // Biến danh sách ghế thành String (VD: A1, A2)
            String seats = String.join(", ", seatList);
            responses.add(
                    BookingHistoryResponse.builder()
                            .bookingId(b.getBookingId())
                            .movieTitle(movieTitle)
                            .showtime(showtime)
                            .seats(seats)
                            .totalAmount(b.getTotalAmount())
                            .status(b.getStatus())
                            .build()
            );
        }
        return responses;
    }

    // Khi người dùng bấm xác nhận đơn hàng và chuyển sang trang thanh toán, kiểm tra xem ghế đã hết hạn chưa
    @Override
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);
        if(booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now())){
            // Nếu hết hạn thì xóa luôn, yêu cầu người dùng chọn lại
            bookingRepository.delete(booking);
            throw new AppException("Đã hết thời gian giữ ghế (10 phút). Vui lòng đặt lại đơn.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getFnbDetailsByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new AppException("Không tìm thấy đơn hàng"));
        List<String> fnbDetails = new ArrayList<>();
        if (booking.getBookingFnbs() != null && !booking.getBookingFnbs().isEmpty()) {
            for (BookingFnB fnB : booking.getBookingFnbs()) {
                fnbDetails.add(fnB.getQuantity() + "x " + fnB.getFnbItem().getName() + " (" + fnB.getSubtotal() + "đ)");
            }
        }
        return fnbDetails;
    }
}
