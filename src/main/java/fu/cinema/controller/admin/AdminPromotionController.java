package fu.cinema.controller.admin;

import fu.cinema.dto.request.PromotionRequest;
import fu.cinema.dto.response.PromotionResponse;
import fu.cinema.entity.Promotion;
import fu.cinema.enums.PromotionStatus;
import fu.cinema.service.NotificationService;
import fu.cinema.service.PromotionService;
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


    private final PromotionService promotionService;
    private final NotificationService notificationService;

    // 1. HIỂN THỊ DANH SÁCH & FORM TẠO MỚI RỖNG
    @GetMapping
    public String viewPromotions(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<PromotionResponse> promotions;

        if (keyword != null && !keyword.trim().isEmpty()) {
            promotions = promotionService.searchPromotions(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            promotions = promotionService.getAllPromotions();
        }

        model.addAttribute("coupons", promotions);

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
            PromotionResponse promotion = promotionService.getPromotionById(id);

            PromotionRequest request = new PromotionRequest();
            request.setId(promotion.getId());
            request.setCode(promotion.getCode());
            request.setDiscountPercentage(promotion.getDiscountPercentage());
            request.setDiscountType(promotion.getDiscountType());
            request.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
            request.setValidFrom(promotion.getValidFrom());
            request.setExpiryDate(promotion.getExpiryDate());
            request.setIsActive(promotion.getStatus() == PromotionStatus.ACTIVE);

            model.addAttribute("coupons", promotionService.getAllPromotions());
            model.addAttribute("coupon", request);

            return "admin/promotion-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    // 3. LƯU DỮ LIỆU TỪ FORM
    @PostMapping("/save")
    public String savePromotion(@ModelAttribute("coupon") PromotionRequest request, RedirectAttributes redirectAttributes) {
        try {
            boolean isNewPromotion = (request.getId() == null || request.getId() <= 0);

            PromotionResponse savedPromotion = promotionService.savePromotion(request);


            if (isNewPromotion && savedPromotion.getStatus() == PromotionStatus.ACTIVE) {
                notificationService.handleNewPromotionNotification(savedPromotion);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã lưu mã giảm giá và kích hoạt thông báo khuyến mãi tới khách hàng!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    // 4. XÓA BẢN GHI
    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã khuyến mãi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa mã khuyến mãi này vì nó đã được sử dụng!");
        }
        return "redirect:/admin/promotions";
    }
}