package fu.cinema.service.impl;

import fu.cinema.entity.Booking;
import fu.cinema.entity.BookingFnB;
import fu.cinema.entity.FnBItem;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.BookingFnBRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.service.BookingFnBService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        // Chặn không cho sửa giỏ hàng khi đơn đã thanh toán hoặc đã hủy
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể thêm bắp nước khi đơn hàng đang chờ thanh toán!");
        }

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
            // Lưu vào DB và đồng thời add vào List để giao diện nhận diện được ngay
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

        // Chặn không cho sửa giỏ hàng khi đơn đã thanh toán hoặc đã hủy
        if (bFnb.getBooking().getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể cập nhật số lượng khi đơn hàng đang chờ thanh toán!");
        }

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

        // Chặn không cho sửa giỏ hàng khi đơn đã thanh toán hoặc đã hủy
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xóa bắp nước khi đơn hàng đang chờ thanh toán!");
        }

        if (booking.getBookingFnbs() != null) {
            booking.getBookingFnbs().removeIf(b -> b.getBookingFnbId().equals(bookingFnbId));
        }

        bookingFnbRepository.delete(bFnb);
        recalculateBookingTotal(booking);
    }

    private void recalculateBookingTotal(Booking booking) {
        // 1. Tính tổng tiền bắp nước
        BigDecimal fnbTotal = BigDecimal.ZERO;
        if (booking.getBookingFnbs() != null) {
            fnbTotal = booking.getBookingFnbs().stream()
                    .map(BookingFnB::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 2. Tính tổng tiền vé
        BigDecimal ticketTotal = BigDecimal.ZERO;
        if (booking.getTickets() != null) {
            for (Ticket t : booking.getTickets()) {
                if (t.getStatus() != TicketStatus.CANCELLED) {
                    ticketTotal = ticketTotal.add(t.getPrice());
                }
            }
        }

        // 3. Tính tổng tiền gốc (Subtotal)
        BigDecimal subtotal = ticketTotal.add(fnbTotal);

        // 4. Tính toán lại mức giảm giá dựa trên Promotion
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (booking.getPromotion() != null) {
            fu.cinema.entity.Promotion promo = booking.getPromotion();

            // BỔ SUNG LỖI THIẾU LOẠI FIXED_AMOUNT
            if ("FIXED_AMOUNT".equalsIgnoreCase(promo.getDiscountType()) || "FIXED_AMOUNT".equals(promo.getDiscountType())) {
                discountAmount = BigDecimal.valueOf(promo.getDiscountPercentage());
                if (discountAmount.compareTo(subtotal) > 0) {
                    discountAmount = subtotal; // Không giảm quá tổng tiền
                }
            }
            // NẾU LÀ GIẢM THEO PHẦN TRĂM (PERCENTAGE)
            else {
                BigDecimal percentage = BigDecimal.valueOf(promo.getDiscountPercentage());

                // THÊM RoundingMode.HALF_UP ĐỂ TRÁNH LỖI CRASH KHI CHIA SỐ THẬP PHÂN VÔ HẠN
                discountAmount = subtotal.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BigDecimal maxDiscount = promo.getMaxDiscountAmount();
                if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0
                        && discountAmount.compareTo(maxDiscount) > 0) {
                    discountAmount = maxDiscount;
                }
            }
        }

        booking.setDiscountAmount(discountAmount);

        // 5. Tính tổng thanh toán cuối cùng
        BigDecimal finalTotal = subtotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        booking.setTotalAmount(finalTotal);
        bookingRepository.save(booking);
    }
}