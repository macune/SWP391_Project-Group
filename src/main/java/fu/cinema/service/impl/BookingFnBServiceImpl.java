package fu.cinema.service.impl;

import fu.cinema.entity.Booking;
import fu.cinema.entity.BookingFnB;
import fu.cinema.entity.FnBItem;
import fu.cinema.repository.BookingFnBRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.service.BookingFnBService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional // Bắt buộc để đảm bảo an toàn dữ liệu khi thao tác nhiều bảng
public class BookingFnBServiceImpl implements BookingFnBService {

    private final BookingRepository bookingRepository;
    private final FnBItemRepository fnbItemRepository;
    private final BookingFnBRepository bookingFnbRepository;

    @Override
    public void addFnBToBooking(Long bookingId, Long itemId, int quantity) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn (Booking) với ID: " + bookingId));

        FnBItem item = fnbItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemId));

        // Kiểm tra xem món này đã có trong giỏ chưa (tránh bị trùng lặp)
        Optional<BookingFnB> existingFnB = booking.getBookingFnbs().stream()
                .filter(b -> b.getFnbItem().getItemId().equals(itemId))
                .findFirst();

        if (existingFnB.isPresent()) {
            // Đã có món này rồi -> Chỉ cộng dồn số lượng lên
            BookingFnB bFnb = existingFnB.get();
            bFnb.setQuantity(bFnb.getQuantity() + quantity);
            bFnb.setSubtotal(bFnb.getPricePerItem().multiply(BigDecimal.valueOf(bFnb.getQuantity())));
            bookingFnbRepository.save(bFnb);
        } else {
            // Chưa có món này -> Tạo dòng mới lưu vào bảng trung gian
            BookingFnB newFnB = BookingFnB.builder()
                    .booking(booking)
                    .fnbItem(item)
                    .quantity(quantity)
                    .pricePerItem(item.getPrice()) // Lưu chốt giá tại thời điểm mua
                    .subtotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .build();
            bookingFnbRepository.save(newFnB);
        }

        // Tự động tính lại tổng tiền hóa đơn
        recalculateBookingTotal(booking);
    }

    @Override
    public void updateFnBQuantity(Long bookingFnbId, int newQuantity) {
        // Nếu số lượng cập nhật <= 0, xóa luôn món đó khỏi đơn
        if (newQuantity <= 0) {
            removeFnBFromBooking(bookingFnbId);
            return;
        }

        BookingFnB bFnb = bookingFnbRepository.findById(bookingFnbId)
                .orElseThrow(() -> new RuntimeException("Chi tiết món không tồn tại trong giỏ hàng"));

        bFnb.setQuantity(newQuantity);
        bFnb.setSubtotal(bFnb.getPricePerItem().multiply(BigDecimal.valueOf(newQuantity)));
        bookingFnbRepository.save(bFnb);

        recalculateBookingTotal(bFnb.getBooking());
    }

    @Override
    public void removeFnBFromBooking(Long bookingFnbId) {
        BookingFnB bFnb = bookingFnbRepository.findById(bookingFnbId)
                .orElseThrow(() -> new RuntimeException("Chi tiết món không tồn tại trong giỏ hàng"));

        Booking booking = bFnb.getBooking();
        bookingFnbRepository.delete(bFnb);


        bookingFnbRepository.flush();

        recalculateBookingTotal(booking);
    }

    //tính toán lại cột totalAmount trong bảng Booking
    private void recalculateBookingTotal(Booking booking) {
        //Tính tổng tiền tất cả bắp nước đang có trong hóa đơn này
        BigDecimal fnbTotal = booking.getBookingFnbs().stream()
                .map(BookingFnB::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tạm thời coi tiền vé ghế là 0
        BigDecimal ticketTotal = BigDecimal.ZERO;

        // Cộng lại thành tổng cuối cùng
        BigDecimal finalTotal = ticketTotal.add(fnbTotal);

        booking.setTotalAmount(finalTotal);
        bookingRepository.save(booking);
    }
}