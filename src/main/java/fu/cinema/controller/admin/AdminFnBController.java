package fu.cinema.controller.admin;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.enums.FnBCategory;
import fu.cinema.service.FnBItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/fnb-items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFnBController {

    private final FnBItemService fnbItemService;

    @GetMapping
    public String fnbItems(@RequestParam(required = false) String name, @RequestParam(required = false) FnBCategory category, Model model) {
        model.addAttribute("items", fnbItemService.searchItems(name, category));
        if (!model.containsAttribute("item")) {
            FnBItemResponse emptyItem = new FnBItemResponse();
            emptyItem.setIsAvailable(true);
            model.addAttribute("item", emptyItem);
        }
        return "admin/fnb-items"; // TRỎ TỚI ADMIN HTML
    }

    @GetMapping("/edit/{id}")
    public String editFnbItem(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            FnBItemResponse item = fnbItemService.GetItemById(id);
            model.addAttribute("items", fnbItemService.GetAllItems());
            model.addAttribute("item", item);
            return "admin/fnb-items";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/fnb-items";
        }
    }

    @PostMapping("/save")
    public String saveFnbItem(@RequestParam(required = false) Long id, @ModelAttribute FnBItemRequest request, Model model) {
        try {
            if (id != null) fnbItemService.updateItem(id, request);
            else fnbItemService.createItem(request);
            return "redirect:/admin/fnb-items";
        } catch (Exception e) {
            FnBItemResponse currentForm = FnBItemResponse.builder()
                    .id(id).name(request.getName()).category(request.getCategory())
                    .price(request.getPrice()).imageUrl(request.getImageUrl()).isAvailable(request.getIsAvailable()).build();
            model.addAttribute("items", fnbItemService.GetAllItems());
            model.addAttribute("item", currentForm);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/fnb-items";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteFnbItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fnbItemService.deleteItem(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("globalError", e.getMessage());
        }
        return "redirect:/admin/fnb-items";
    }
}