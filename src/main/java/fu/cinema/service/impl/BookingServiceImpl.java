package fu.cinema.service.impl;

import fu.cinema.dto.request.SeatLockRequest;
import fu.cinema.dto.response.*;
import fu.cinema.entity.*;
import fu.cinema.enums.*;
import fu.cinema.exception.*;
import fu.cinema.repository.*;
import fu.cinema.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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

    // Tính tiền cho từng ghế (giá vé = giá base + giá ghế)
    private BigDecimal calculateSeatPrice(SeatType seatType, BigDecimal basePrice){
        if(seatType == SeatType.VIP){
            return basePrice.add(BigDecimal.valueOf(15000));
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
                bookingRepository.delete(booking);
            }
        }
    }

    // Hàm trả về thông tin người dùng đặt vé (thông tin vé, tổng giá tiền,...)
    @Override
    @Transactional(readOnly = true)
    public ReviewOrderResponse getReviewOrderData(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);

        if(booking.getStatus()!=BookingStatus.PENDING){
            throw new AppException("Đơn hàng không hợp lệ hoặc đã hoàn tất.");
        }

        Showtime showtime = booking.getTickets().get(0).getShowtime();

        // Duyệt qua những ticket người dùng đã đặt để lấy tên ghế (VD: A1, A2)
        List<String> seatNames = new ArrayList<>();
        for(Ticket ticket : booking.getTickets()){
            seatNames.add(ticket.getSeat().getRowCode() + ticket.getSeat().getNumber());
        }
        // Giá tiền vé
        BigDecimal ticketTotal = booking.getTotalAmount();

        // Giá tiền fnb
        List<String> fnbDetails = new ArrayList<>();
        BigDecimal fnbTotal = BigDecimal.ZERO;
        if(booking.getBookingFnbs()!=null && !booking.getBookingFnbs().isEmpty()){
            for(BookingFnB bookingFnB : booking.getBookingFnbs()){
                fnbDetails.add(bookingFnB.getQuantity() + "x" + bookingFnB.getFnbItem().getName()+ "(" + bookingFnB.getSubtotal() + "đ)");
                fnbTotal = fnbTotal.add(bookingFnB.getSubtotal());
            }
        }
        // Tổng giá tiền cần trả (chưa áp mã giảm giá)
        BigDecimal totalAmountWithoutDiscount = ticketTotal.add(fnbTotal);
        BigDecimal discountAmount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;

        // Tổng giá tiền cuối cùng người dùng phải trả
        BigDecimal finalAmount = ticketTotal.add(fnbTotal).subtract(discountAmount);
        if(finalAmount.compareTo(BigDecimal.ZERO)<0){
            finalAmount = BigDecimal.ZERO;
        }

        // Trả về thông tin cần thiết cho frontend
        return ReviewOrderResponse.builder()
                .bookingId(booking.getBookingId())
                .movieTitle(showtime.getMovie().getTitle())
                .posterUrl(showtime.getMovie().getPosterUrl())
                .branchName(showtime.getHall().getBranch().getBranchName())
                .hallName(showtime.getHall().getHallName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
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

        // Kiểm tra xem mã này còn khả dụng không
        if(promotion.getStatus()!=PromotionStatus.ACTIVE){
            throw new AppException("Mã khuyến mãi này không còn khả dụng.");
        }
        if(promotion.getValidFrom()!=null && promotion.getValidFrom().isAfter(LocalDateTime.now())){
            throw new AppException("Mã khuyến mãi này chưa đến ngày bắt đầu sử dụng.");
        }
        if(promotion.getExpiryDate()!=null && promotion.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new AppException(("Mã khuyến mãi này đã hết hạn sử dụng."));
        }

        // Tính tổng tiền người dùng phải trả (chưa có discount)
        BigDecimal ticketTotal = booking.getTotalAmount();
        BigDecimal fnbTotal = BigDecimal.ZERO;
        if(booking.getBookingFnbs()!=null && !booking.getBookingFnbs().isEmpty()){
            for(BookingFnB fnB : booking.getBookingFnbs()){
                fnbTotal = fnbTotal.add(fnB.getSubtotal());
            }
        }
        BigDecimal totalAmountWithoutDiscount = ticketTotal.add(fnbTotal);

        // Tính giảm giá (discount được tính dựa trên tiền vé + tiền fnb)
        BigDecimal discount = BigDecimal.ZERO;
        if(DiscountType.FIXED_AMOUNT.toString().equals(promotion.getDiscountType())){
            discount = BigDecimal.valueOf(promotion.getDiscountPercentage());
            // Discount không thể lớn hơn
            if(discount.compareTo(totalAmountWithoutDiscount)>0){
                discount = totalAmountWithoutDiscount;
            }
        }else{
            discount = totalAmountWithoutDiscount
                    .multiply(BigDecimal.valueOf(promotion.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100)));
            // Hạn mức tối đa được phép giảm
            if(promotion.getMaxDiscountAmount()!=null
                    && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO)>0
                    && promotion.getMaxDiscountAmount().compareTo(discount)<0){
                discount = promotion.getMaxDiscountAmount();
            }
        }

        booking.setPromotion(promotion);
        booking.setDiscountAmount(discount);
        bookingRepository.save(booking);
    }

    // Hàm chạy khi người dùng bấm hủy đơn (giải phóng ghế)
    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(BookingNotFoundException::new);

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
            String movieTitle = (!b.getTickets().isEmpty()) ? b.getTickets().get(0).getShowtime().getMovie().getTitle() : "---";

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
        String movie = !b.getTickets().isEmpty() ? b.getTickets().get(0).getShowtime().getMovie().getTitle() : "---";
        String hall = !b.getTickets().isEmpty() ? b.getTickets().get(0).getShowtime().getHall().getHallName() : "---";
        LocalDateTime time = !b.getTickets().isEmpty() ? b.getTickets().get(0).getShowtime().getStartTime() : null;
        List<String> seats = new ArrayList<>();
        for (Ticket t : b.getTickets()) {
            seats.add(t.getSeat().getRowCode() + t.getSeat().getNumber());
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

    // Chỉnh sửa trạng thái đơn hàng (dành riêng cho Staff)
    @Override
    public void updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById((bookingId)).orElseThrow(BookingNotFoundException::new);

        // Nếu booking đã bị hủy, ngăn không cho đổi trạng thái
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException("Đơn đặt vé này đã bị hủy, không thể thay đổi trạng thái!");
        }

        booking.setStatus(newStatus);
        // Đơn hàng hết hạn thì toàn bộ vé ứng với đơn hàng đó về trạng thái Cancelled
        if(newStatus == BookingStatus.CANCELLED){
            for(Ticket ticket : booking.getTickets()){
                ticket.setStatus(TicketStatus.CANCELLED);
            }
        }

        // Tương tự về trạng thái Booked
        if(newStatus == BookingStatus.CONFIRMED){
            for(Ticket ticket : booking.getTickets()){
                ticket.setStatus(TicketStatus.BOOKED);
            }
        }
        bookingRepository.save(booking);
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
}
