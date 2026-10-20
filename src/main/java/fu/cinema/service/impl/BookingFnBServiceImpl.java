package fu.cinema.service.impl;

import fu.cinema.entity.Booking;
import fu.cinema.entity.BookingFnB;
import fu.cinema.entity.FnBItem;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.BookingFnBRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.service.BookingFnBService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingFnBServiceImpl implements BookingFnBService {

    private final BookingRepository bookingRepository;
    private final FnBItemRepository fnbItemRepository;
    private final BookingFnBRepository bookingFnbRepository;

    @Override
    public void addFnBToBooking(Long bookingId, Long itemId, int quantity) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với ID: " + bookingId));
        FnBItem item = fnbItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemId));

        if (booking.getBookingFnbs() == null) {
            booking.setBookingFnbs(new ArrayList<>());
        }

        Optional<BookingFnB> existingFnB = booking.getBookingFnbs().stream()
                .filter(b -> b.getFnbItem().getItemId().equals(itemId))
                .findFirst();

        if (existingFnB.isPresent()) {
            BookingFnB bFnb = existingFnB.get();
            bFnb.setQuantity(bFnb.getQuantity() + quantity);
            bFnb.setSubtotal(bFnb.getPricePerItem().multiply(BigDecimal.valueOf(bFnb.getQuantity())));
            bookingFnbRepository.save(bFnb);
        } else {
            BookingFnB newFnB = BookingFnB.builder()
                    .booking(booking)
                    .fnbItem(item)
                    .quantity(quantity)
                    .pricePerItem(item.getPrice())
                    .subtotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .build();
            // Lưu vào DB và đồng thời add vào List trên RAM để giao diện nhận diện được ngay
            newFnB = bookingFnbRepository.save(newFnB);
            booking.getBookingFnbs().add(newFnB);
        }

        recalculateBookingTotal(booking);
    }

    @Override
    public void updateFnBQuantity(Long bookingFnbId, int newQuantity) {
        if (newQuantity <= 0) {
            removeFnBFromBooking(bookingFnbId);
            return;
        }
        BookingFnB bFnb = bookingFnbRepository.findById(bookingFnbId)
                .orElseThrow(() -> new RuntimeException("Chi tiết món không tồn tại"));

        bFnb.setQuantity(newQuantity);
        bFnb.setSubtotal(bFnb.getPricePerItem().multiply(BigDecimal.valueOf(newQuantity)));
        bookingFnbRepository.save(bFnb);
        recalculateBookingTotal(bFnb.getBooking());
    }

    @Override
    public void removeFnBFromBooking(Long bookingFnbId) {
        BookingFnB bFnb = bookingFnbRepository.findById(bookingFnbId)
                .orElseThrow(() -> new RuntimeException("Chi tiết món không tồn tại"));
        Booking booking = bFnb.getBooking();

        // Gỡ khỏi list trên RAM
        if (booking.getBookingFnbs() != null) {
            booking.getBookingFnbs().removeIf(b -> b.getBookingFnbId().equals(bookingFnbId));
        }

        bookingFnbRepository.delete(bFnb);
        recalculateBookingTotal(booking);
    }

    private void recalculateBookingTotal(Booking booking) {
        BigDecimal fnbTotal = BigDecimal.ZERO;
        if (booking.getBookingFnbs() != null) {
            fnbTotal = booking.getBookingFnbs().stream()
                    .map(BookingFnB::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal ticketTotal = BigDecimal.ZERO;
        if (booking.getTickets() != null) {
            for (Ticket t : booking.getTickets()) {
                if (t.getStatus() != TicketStatus.CANCELLED) {
                    ticketTotal = ticketTotal.add(t.getPrice());
                }
            }
        }

        BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalTotal = ticketTotal.add(fnbTotal).subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        booking.setTotalAmount(finalTotal);
        bookingRepository.save(booking);
    }
}