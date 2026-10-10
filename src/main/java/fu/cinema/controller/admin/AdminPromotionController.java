package fu.cinema.controller.admin;

import fu.cinema.entity.Coupon;
import fu.cinema.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final CouponRepository couponRepository;

    //Hiển thị danh sách mã giảm giá
    @GetMapping
    public String viewPromotions(Model model) {
        List<Coupon> coupons = couponRepository.findAll();
        model.addAttribute("coupons", coupons);


        if (!model.containsAttribute("coupon")) {
            model.addAttribute("coupon", new Coupon());
        }
        return "admin/promotion-management";
    }

    //Thêm mới hoặc Cập nhật mã
    @PostMapping("/save")
    public String savePromotion(@ModelAttribute Coupon coupon, RedirectAttributes redirectAttributes) {
        try {

            if (coupon.getId() == null) {
                coupon.setIsActive(true);
            }
            couponRepository.save(coupon);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Tên mã (Code) có thể đã bị trùng!");
        }
        return "redirect:/admin/promotions";
    }

    // Xóa mã
    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        couponRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã giảm giá!");
        return "redirect:/admin/promotions";
    }
}