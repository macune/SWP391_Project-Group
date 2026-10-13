package fu.cinema.controller.admin;

import fu.cinema.entity.Promotion;
import fu.cinema.repository.PromotionRepository; // Giả sử bạn đang dùng Repository trực tiếp
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

    // 1. CẬP NHẬT HÀM GET ĐỂ HỖ TRỢ TÌM KIẾM
    @GetMapping
    public String viewPromotions(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Promotion> promotions;

        // Logic tìm kiếm theo Code
        if (keyword != null && !keyword.trim().isEmpty()) {
            // Đảm bảo trong PromotionRepository bạn đã viết hàm findByCodeContainingIgnoreCase
            promotions = promotionRepository.findByCodeContainingIgnoreCase(keyword.trim());
            model.addAttribute("keyword", keyword);
        } else {
            promotions = promotionRepository.findAll();
        }

        model.addAttribute("coupons", promotions);

        if (!model.containsAttribute("coupon")) {
            Promotion newCoupon = new Promotion();
            newCoupon.setIsActive(true);
            model.addAttribute("coupon", newCoupon);
        }
        return "admin/promotion-management";
    }

    // 2. THÊM HÀM EDIT ĐỂ LOAD DỮ LIỆU LÊN FORM
    @GetMapping("/edit/{id}")
    public String editPromotion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Promotion promotion = promotionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mã khuyến mãi!"));

            // Map trạng thái từ Enum sang biến checkbox giao diện
            promotion.setIsActive(promotion.getStatus().name().equals("ACTIVE"));

            // Lấy lại danh sách để hiện thị bảng bên cạnh
            model.addAttribute("coupons", promotionRepository.findAll());
            model.addAttribute("coupon", promotion);
            return "admin/promotion-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    // Hàm Save và Delete bạn giữ nguyên như cũ
    @PostMapping("/save")
    public String savePromotion(@ModelAttribute("coupon") Promotion promotion, RedirectAttributes redirectAttributes) {
        try {
            if (promotion.getId() != null && promotion.getId() == 0) {
                promotion.setId(null);
            }

            if (promotion.getStatus() == null) {
                promotion.setStatus(fu.cinema.enums.PromotionStatus.ACTIVE);
            }

            promotionRepository.save(promotion);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu mã: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã khuyến mãi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa mã khuyến mãi này vì nó đã được sử dụng!");
        }
        return "redirect:/admin/promotions";
    }
}