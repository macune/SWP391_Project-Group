package fu.cinema.service.impl;

import fu.cinema.dto.request.PromotionRequest;
import fu.cinema.dto.response.PromotionResponse;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;

    //Chuyển đổi Entity sang DTO Response
    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .discountPercentage(promotion.getDiscountPercentage())
                .discountType(promotion.getDiscountType())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .validFrom(promotion.getValidFrom())
                .expiryDate(promotion.getExpiryDate())
                .status(promotion.getStatus())
                .build();
    }

    // Lấy danh sách mã khuyến mãi còn hạn và đang mở
    @Override
    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findByStatusAndExpiryDateAfter(
                PromotionStatus.ACTIVE,
                LocalDateTime.now()
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Lấy toàn bộ mã khuyến mãi
    @Override
    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Tìm kiếm mã khuyến mãi
    @Override
    public List<PromotionResponse> searchPromotions(String keyword) {
        return promotionRepository.findByCodeContainingIgnoreCase(keyword.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Lấy thông tin chi tiết 1 mã khuyến mãi
    @Override
    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã khuyến mãi!"));
        return toResponse(promotion);
    }

    // Lưu mã khuyến mãi: 2 trường hợp Thêm mới và Cập nhật
    @Override
    public PromotionResponse savePromotion(PromotionRequest request) {
        boolean isNewPromotion = (request.getId() == null || request.getId() <= 0);
        Promotion promotion;
        String cleanCode = request.getCode().trim();

        if (isNewPromotion) {
            // KIỂM TRA TRÙNG LẶP KHI TẠO MỚI
            if (promotionRepository.existsByCodeIgnoreCase(cleanCode)) {
                throw new RuntimeException("Mã khuyến mãi '" + cleanCode + "' đã tồn tại! Vui lòng nhập mã khác.");
            }
            promotion = new Promotion();
        } else {
            // TÌM BẢN GHI CŨ KHI CẬP NHẬT
            promotion = promotionRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mã khuyến mãi để cập nhật!"));

            // KIỂM TRA TRÙNG LẶP KHI CẬP NHẬT
            if (!promotion.getCode().equalsIgnoreCase(cleanCode)
                    && promotionRepository.existsByCodeIgnoreCase(cleanCode)) {
                throw new RuntimeException("Mã khuyến mãi '" + cleanCode + "' đã tồn tại! Vui lòng nhập mã khác.");
            }
        }

        // GẮN DỮ LIỆU TỪ FORM VÀO ENTITY
        promotion.setCode(cleanCode);
        promotion.setDiscountPercentage(request.getDiscountPercentage());
        promotion.setDiscountType(request.getDiscountType() != null ? request.getDiscountType() : "PERCENTAGE");
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setValidFrom(request.getValidFrom());
        promotion.setExpiryDate(request.getExpiryDate());

        promotion.setStatus((request.getIsActive() != null && request.getIsActive())
                ? PromotionStatus.ACTIVE
                : PromotionStatus.DISABLED);

        Promotion savedPromotion = promotionRepository.save(promotion);
        return toResponse(savedPromotion);
    }

    // Xóa mã khuyến mãi khỏi DB
    @Override
    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }
}