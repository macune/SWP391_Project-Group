package fu.cinema.service.impl;

import fu.cinema.entity.Promotion;
import fu.cinema.enums.PromotionStatus;
import fu.cinema.repository.PromotionRepository;
import fu.cinema.service.PromotionService;
import lombok.RequiredArgsConstructor;
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

    @Override
    public List<Promotion> getActivePromotions() {
        // Sửa điều kiện cũ (p.isActive = true) thành lọc theo status và thời gian hiệu lực
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
        if (!promo.getIsActive()) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá này đã bị khóa!");
            return response;
        }
        if (promo.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.put("valid", false);
            response.put("message", "Mã giảm giá đã hết hạn!");
            return response;
        }

        response.put("valid", true);
        response.put("message", "Áp dụng mã thành công!");
        response.put("discountPercentage", promo.getDiscountPercentage());
        response.put("maxDiscountAmount", promo.getMaxDiscountAmount());
        return response;
    }
}