package fu.cinema.service.impl;

import fu.cinema.dto.request.TicketInitRequest;
import fu.cinema.dto.response.TicketResponse;
import fu.cinema.entity.*;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.Role;
import fu.cinema.enums.SeatType;
import fu.cinema.enums.TicketStatus;
import fu.cinema.exception.AppException;
import fu.cinema.exception.ErrorCode;
import fu.cinema.mapper.TicketMapper;
import fu.cinema.repository.*;
import fu.cinema.service.TicketService;
import fu.cinema.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final AccountRepository accountRepository;
    private final TicketMapper ticketMapper;

    @Override
    public BigDecimal calculateTicketPrice(Showtime showtime, Seat seat) {
        BigDecimal basePrice = showtime.getBasePrice() != null ? showtime.getBasePrice() : BigDecimal.ZERO;
        SeatType seatType = seat.getType() != null ? seat.getType() : SeatType.STANDARD;
        return basePrice.add(seatType.getSurcharge());
    }

    @Override
    @Transactional
    public List<TicketResponse> initializeTickets(TicketInitRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS);
        }

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        List<Ticket> createdTickets = new ArrayList<>();
        BigDecimal ticketsTotal = BigDecimal.ZERO;

        for (Seat seat : seats) {
            boolean alreadyTaken = ticketRepository.existsByShowtime_ShowtimeIdAndSeat_SeatIdAndStatusNot(
                    showtime.getShowtimeId(), seat.getSeatId(), TicketStatus.CANCELLED);
            if (alreadyTaken) {
                throw new AppException(ErrorCode.SEAT_ALREADY_TAKEN);
            }

            BigDecimal ticketPrice = calculateTicketPrice(showtime, seat);
            ticketsTotal = ticketsTotal.add(ticketPrice);

            TicketStatus targetStatus = request.isIssueImmediately()
                    ? TicketStatus.BOOKED
                    : TicketStatus.LOCKED;

            String qrCodeValue = request.isIssueImmediately()
                    ? QrCodeUtil.generateOfficialQrCode(booking.getBookingId(), showtime.getShowtimeId(), seat.getSeatId())
                    : QrCodeUtil.generateLockedPlaceholder(booking.getBookingId(), showtime.getShowtimeId(), seat.getSeatId());

            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .showtime(showtime)
                    .seat(seat)
                    .price(ticketPrice)
                    .status(targetStatus)
                    .qrCode(qrCodeValue)
                    .build();

            createdTickets.add(ticket);
        }

        ticketRepository.saveAll(createdTickets);

        // Nếu đơn đặt vé chưa được tính tổng tiền thì cập nhật tổng tiền vé
        if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
            booking.setTotalAmount(ticketsTotal.subtract(discount).max(BigDecimal.ZERO));
        }
        if (request.isIssueImmediately()) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        bookingRepository.save(booking);

        return ticketRepository.findAllByBookingIdWithDetails(booking.getBookingId())
                .stream()
                .map(ticketMapper::toTicketResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<TicketResponse> generateETicketsForBooking(Long bookingId) {
        return generateETicketsForBooking(bookingId, null, null);
    }

    @Override
    @Transactional
    public List<TicketResponse> generateETicketsForBooking(Long bookingId, Long showtimeId, List<Long> seatIds) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS);
        }

        List<Ticket> existingTickets = ticketRepository.findAllByBookingIdWithDetails(bookingId);

        // TRƯỜNG HỢP B: Booking chưa có bản ghi Ticket nào -> Khởi tạo mới hoàn toàn và phát hành QR ngay
        if (existingTickets.isEmpty()) {
            if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
                throw new AppException(ErrorCode.TICKET_NOT_FOUND);
            }
            TicketInitRequest initRequest = TicketInitRequest.builder()
                    .bookingId(bookingId)
                    .showtimeId(showtimeId)
                    .seatIds(seatIds)
                    .issueImmediately(true)
                    .build();
            return initializeTickets(initRequest);
        }

        // TRƯỜNG HỢP A: Booking đã có sẵn các bản ghi Ticket (ví dụ ở trạng thái LOCKED)
        // -> Chuyển trạng thái sang BOOKED, tính chuẩn giá vé theo loại ghế và sinh mã QR điện tử chính thức
        for (Ticket ticket : existingTickets) {
            if (ticket.getStatus() == TicketStatus.CANCELLED) {
                continue;
            }

            BigDecimal expectedPrice = calculateTicketPrice(ticket.getShowtime(), ticket.getSeat());
            if (ticket.getPrice() == null || ticket.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                ticket.setPrice(expectedPrice);
            }

            if (ticket.getStatus() == TicketStatus.LOCKED) {
                ticket.setStatus(TicketStatus.BOOKED);
            }

            // Nếu qrCode đang là mã giữ chỗ tạm (LOCK-...) hoặc chưa phải mã chính thức (CF-TKT-...) thì sinh mới
            if (ticket.getQrCode() == null || !ticket.getQrCode().startsWith("CF-TKT-")) {
                String officialQr = QrCodeUtil.generateOfficialQrCode(
                        booking.getBookingId(),
                        ticket.getShowtime().getShowtimeId(),
                        ticket.getSeat().getSeatId()
                );
                ticket.setQrCode(officialQr);
            }
        }

        ticketRepository.saveAll(existingTickets);

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        return existingTickets.stream()
                .map(ticketMapper::toTicketResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketDetail(Long ticketId, String currentUsername) {
        Ticket ticket = ticketRepository.findByIdWithFullDetails(ticketId)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        validateTicketAccess(ticket.getBooking(), currentUsername);
        return ticketMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByBooking(Long bookingId, String currentUsername) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        validateTicketAccess(booking, currentUsername);

        List<Ticket> tickets = ticketRepository.findAllByBookingIdWithDetails(bookingId);
        return tickets.stream()
                .map(ticketMapper::toTicketResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(String username) {
        return ticketRepository.findAllByCustomerUsernameWithDetails(username)
                .stream()
                .map(ticketMapper::toTicketResponse)
                .toList();
    }

    /**
     * Kiểm tra quyền truy cập vé:
     * - Chủ sở hữu đơn đặt vé (Customer) được phép xem.
     * - Nhân viên (STAFF), Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) được phép xem để hỗ trợ tại quầy.
     */
    private void validateTicketAccess(Booking booking, String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new AppException(ErrorCode.TICKET_ACCESS_DENIED);
        }
        if (booking.getAccount() != null && currentUsername.equalsIgnoreCase(booking.getAccount().getUsername())) {
            return;
        }
        Account viewer = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (viewer.getRole() == Role.STAFF || viewer.getRole() == Role.MANAGER || viewer.getRole() == Role.ADMIN) {
            return;
        }
        throw new AppException(ErrorCode.TICKET_ACCESS_DENIED);
    }
}