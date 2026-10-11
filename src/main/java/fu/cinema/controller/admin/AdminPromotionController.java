package fu.cinema.controller.admin;

import fu.cinema.entity.Promotion;
import fu.cinema.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionRepository promotionRepository;

    @GetMapping
    public String viewPromotions(Model model) {
        List<Promotion> promotions = promotionRepository.findAll();
        model.addAttribute("coupons", promotions);
        if (!model.containsAttribute("coupon")) {
            model.addAttribute("coupon", new Promotion());
        }
        return "admin/promotion-management";
    }

    @PostMapping("/save")
    public String savePromotion(@ModelAttribute("coupon") Promotion promotion, RedirectAttributes redirectAttributes) {
        try {
            promotionRepository.save(promotion);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu mã (Code có thể đã tồn tại)!");
        }
        return "redirect:/admin/promotions";
    }

    // Đổi lại thành xóa theo ID để khớp với Database
    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        promotionRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã giảm giá!");
        return "redirect:/admin/promotions";
    }
}