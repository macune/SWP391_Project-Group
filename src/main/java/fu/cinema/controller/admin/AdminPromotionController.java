package fu.cinema.controller.admin;

import fu.cinema.dto.request.PromotionRequest;
import fu.cinema.entity.Promotion;
import fu.cinema.enums.PromotionStatus;
import fu.cinema.repository.PromotionRepository;
import fu.cinema.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionRepository promotionRepository;
    private final NotificationService notificationService;

    // 1. HIỂN THỊ DANH SÁCH & FORM TẠO MỚI RỖNG
    @GetMapping
    public String viewPromotions(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Promotion> promotions;

        // Lọc theo từ khóa (nếu có)
        if (keyword != null && !keyword.trim().isEmpty()) {
            promotions = promotionRepository.findByCodeContainingIgnoreCase(keyword.trim());
            model.addAttribute("keyword", keyword);
        } else {
            promotions = promotionRepository.findAll();
        }

        model.addAttribute("coupons", promotions); // Đẩy danh sách ra bảng

        // Chuẩn bị form tạo mới rỗng (Mặc định: Đang mở)
        if (!model.containsAttribute("coupon")) {
            PromotionRequest newCouponRequest = new PromotionRequest();
            newCouponRequest.setIsActive(true);
            model.addAttribute("coupon", newCouponRequest);
        }
        return "admin/promotion-management";
    }

    // 2. LOAD DỮ LIỆU BẢN GHI CŨ LÊN FORM ĐỂ SỬA
    @GetMapping("/edit/{id}")
    public String editPromotion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Lấy mã cũ từ DB
            Promotion promotion = promotionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mã khuyến mãi!"));

            // Đổ dữ liệu từ Entity sang form DTO
            PromotionRequest request = new PromotionRequest();
            request.setId(promotion.getId());
            request.setCode(promotion.getCode());
            request.setDiscountPercentage(promotion.getDiscountPercentage());
            request.setDiscountType(promotion.getDiscountType());
            request.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
            request.setValidFrom(promotion.getValidFrom());
            request.setExpiryDate(promotion.getExpiryDate());
            request.setIsActive(promotion.getStatus() == PromotionStatus.ACTIVE);

            model.addAttribute("coupons", promotionRepository.findAll());
            model.addAttribute("coupon", request);

            return "admin/promotion-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    // 3. LƯU DỮ LIỆU TỪ FORM (XỬ LÝ CẢ THÊM MỚI VÀ CẬP NHẬT)
    @PostMapping("/save")
    public String savePromotion(@ModelAttribute("coupon") PromotionRequest request, RedirectAttributes redirectAttributes) {
        try {
            boolean isNewPromotion = (request.getId() == null || request.getId() <= 0);
            Promotion promotion;

            if (!isNewPromotion) {
                promotion = promotionRepository.findById(request.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy mã khuyến mãi để cập nhật!"));
            } else {
                promotion = new Promotion();
            }

            // Gắn dữ liệu từ form vào Entity
            promotion.setCode(request.getCode());
            promotion.setDiscountPercentage(request.getDiscountPercentage());
            promotion.setDiscountType(request.getDiscountType() != null ? request.getDiscountType() : "PERCENTAGE");
            promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
            promotion.setValidFrom(request.getValidFrom());
            promotion.setExpiryDate(request.getExpiryDate());

            promotion.setStatus((request.getIsActive() != null && request.getIsActive())
                    ? PromotionStatus.ACTIVE
                    : PromotionStatus.DISABLED);

            Promotion savedPromotion = promotionRepository.save(promotion);

            // FE-10.4: Nếu là tạo mã mới và đang ACTIVE -> Tự động gửi thông báo hoặc hẹn giờ JOB-04
            if (isNewPromotion && savedPromotion.getStatus() == PromotionStatus.ACTIVE) {
                notificationService.handleNewPromotionNotification(savedPromotion);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã lưu mã giảm giá và kích hoạt thông báo khuyến mãi tới khách hàng!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu mã: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    // 4. XÓA BẢN GHI
    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã khuyến mãi!");
        } catch (Exception e) {
            // Chặn lỗi khóa ngoại nếu mã giảm giá đã từng được áp dụng cho đơn đặt vé
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa mã khuyến mãi này vì nó đã được sử dụng!");
        }
        return "redirect:/admin/promotions";
    }
}