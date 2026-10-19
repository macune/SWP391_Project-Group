package fu.cinema.service;

public interface BookingFnBService {
    // Khai báo 3 hàm dùng để Thêm, Sửa, Xóa giỏ hàng
    void addFnBToBooking(Long bookingId, Long itemId, int quantity);

    void updateFnBQuantity(Long bookingFnbId, int newQuantity);

    void removeFnBFromBooking(Long bookingFnbId);
}