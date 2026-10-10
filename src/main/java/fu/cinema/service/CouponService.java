package fu.cinema.service;

import fu.cinema.dto.response.CouponResponse;
import fu.cinema.entity.Coupon;

import java.util.List;

public interface CouponService {

    CouponResponse validateCoupon(String code);
    List<Coupon> getActiveCoupons();
}