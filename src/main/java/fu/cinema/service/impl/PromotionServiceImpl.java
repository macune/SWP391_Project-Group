package fu.cinema.service.impl;

import fu.cinema.entity.Account;
import fu.cinema.entity.Promotion;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.PromotionStatus;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.PromotionRepository;
import fu.cinema.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByStatusAndExpiryDateAfter(
                PromotionStatus.ACTIVE,
                LocalDateTime.now()
        );
    }

    @Override
    public Object validatePromotion(String code) {
        Map<String, Object> response = new HashMap<>();

        Optional<Promotion> opt = promotionRepository.findByCode(code);

        if (opt.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá không tồn tại!");
            return response;
        }

        Promotion promo = opt.get();

        if (promo.getStatus() != PromotionStatus.ACTIVE) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá này đã bị khóa hoặc không khả dụng!");
            return response;
        }

        if (promo.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá đã hết hạn!");
            return response;
        }

        if (promo.getValidFrom() != null && promo.getValidFrom().isAfter(LocalDateTime.now())) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá này chưa đến ngày sử dụng!");
            return response;
        }

        // =================================================================
        // CHỐT CHẶN: KIỂM TRA MỖI TÀI KHOẢN CHỈ ĐƯỢC DÙNG 1 LẦN
        // =================================================================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Optional<Account> currentUserOpt = accountRepository.findByUsername(auth.getName());

            if (currentUserOpt.isPresent()) {
                Account currentUser = currentUserOpt.get();

                // Kiểm tra Role
                boolean isStaffOrManager = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_MANAGER"));

                // Chỉ chặn nếu là Khách hàng (Customer)
                if (!isStaffOrManager) {
                    // Đã sửa: Gọi hàm 3 tham số để không bị báo lỗi thiếu bookingId
                    boolean alreadyUsed = bookingRepository.existsByAccount_AccountIdAndPromotion_IdAndStatusNot(
                            currentUser.getAccountId(),
                            promo.getId(),
                            BookingStatus.CANCELLED
                    );

                    if (alreadyUsed) {
                        response.put("valid", false);
                        response.put("message", "Tài khoản của bạn đã sử dụng mã giảm giá này rồi!");
                        return response;
                    }
                }
            }
        } else {
            response.put("valid", false);
            response.put("message", "Vui lòng đăng nhập để sử dụng mã khuyến mãi này!");
            return response;
        }
        // =================================================================

        response.put("valid", true);
        response.put("message", "Áp dụng mã thành công!");
        response.put("discountPercentage", promo.getDiscountPercentage());
        response.put("maxDiscountAmount", promo.getMaxDiscountAmount());

        // FIX BUG: Trả về loại khuyến mãi để frontend biết mà tính toán không bị âm tiền
        response.put("discountType", promo.getDiscountType());

        return response;
    }
}