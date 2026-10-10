package fu.cinema.repository;

import fu.cinema.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // Tìm kiếm mã giảm giá dựa vào chuỗi code do Staff nhập vào
    Optional<Coupon> findByCode(String code);

    // Kiểm tra xem một mã có tồn tại hay không
    boolean existsByCode(String code);
    // Lấy danh sách các mã đang mở và còn hạn sử dụng
    List<Coupon> findByIsActiveTrueAndExpiryDateAfter(LocalDateTime now);
}