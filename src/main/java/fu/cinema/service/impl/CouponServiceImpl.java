package fu.cinema.service.impl;

import fu.cinema.dto.response.CouponResponse;
import fu.cinema.entity.Coupon;
import fu.cinema.repository.CouponRepository;
import fu.cinema.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findByIsActiveTrueAndExpiryDateAfter(LocalDateTime.now());
    }
    @Override
    public CouponResponse validateCoupon(String code) {
        // Tìm mã trong Database
        Optional<Coupon> couponOpt = couponRepository.findByCode(code);

        // Nếu không tìm thấy
        if (couponOpt.isEmpty()) {
            return CouponResponse.builder()
                    .valid(false)
                    .message("Mã giảm giá không tồn tại!")
                    .build();
        }

        Coupon coupon = couponOpt.get();

        // Kiểm tra xem mã có bị khóa không
        if (!coupon.getIsActive()) {
            return CouponResponse.builder()
                    .valid(false)
                    .message("Mã giảm giá này đã bị khóa!")
                    .build();
        }

        // Kiểm tra hạn sử dụng
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            return CouponResponse.builder()
                    .valid(false)
                    .message("Mã giảm giá đã hết hạn!")
                    .build();
        }

        // Vượt qua mọi bài kiểm tra -> Hợp lệ
        return CouponResponse.builder()
                .valid(true)
                .message("Áp dụng mã thành công!")
                .discountPercentage(coupon.getDiscountPercentage())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .build();
    }
}