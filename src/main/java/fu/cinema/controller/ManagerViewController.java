package fu.cinema.controller;
import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.enums.FnBCategory;
import fu.cinema.exception.DuplicateFnBItemException;
import fu.cinema.service.FnBItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerViewController {

    private final FnBItemService fnBItemService;



    // =========================
    // EDIT
    // =========================

    @GetMapping("/fnb-items/edit/{id}")
    public String editItem(
            @PathVariable Long id,
            Model model) {

        FnBItemResponse response =
                fnBItemService.GetItemById(id);

        FnBItemRequest request = new FnBItemRequest();

        request.setId(response.getId());
        request.setName(response.getName());
        request.setCategory(response.getCategory());
        request.setPrice(response.getPrice());
        request.setIsAvailable(response.getIsAvailable());
        request.setImageUrl(response.getImageUrl());

        model.addAttribute("item", request);

        // Vẫn cần danh sách để hiển thị table
        model.addAttribute(
                "items",
                fnBItemService.GetAllItems()
        );

        return "manager/fnb-items";
    }


    // =========================
    // CREATE + EDIT
    // =========================

    @PostMapping("/fnb-items/save")
    public String saveItem(
            @Valid @ModelAttribute("item") FnBItemRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {

            model.addAttribute(
                    "items",
                    fnBItemService.GetAllItems()
            );

            return "manager/fnb-items";
        }


        // =========================
        // CREATE
        // =========================

        if (request.getId() == null) {

            fnBItemService.createItem(request);

        }

        // =========================
        // EDIT
        // =========================

        else {

            fnBItemService.updateItem(
                    request.getId(),
                    request
            );

        }

        return "redirect:/manager/fnb-items";
    }


    // =========================
    // DELETE
    // =========================

    @PostMapping("/fnb-items/delete/{id}")
    public String deleteItem(
            @PathVariable Long id) {

        fnBItemService.deleteItem(id);

        return "redirect:/manager/fnb-items";
    }

    @GetMapping("/fnb-items")
    public String fnbItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) FnBCategory category,
            Model model) {

        List<FnBItemResponse> items =
                fnBItemService.searchItems(name, category);

        model.addAttribute("items", items);

        FnBItemRequest item = new FnBItemRequest();
        item.setName(name);

        if (category != null) {
            item.setCategory(category);
        }

        model.addAttribute("item", item);

        return "manager/fnb-items";
    }
    @ExceptionHandler(DuplicateFnBItemException.class)
    public String handleDuplicateFnBItem(
            DuplicateFnBItemException ex,
            Model model) {

        model.addAttribute("errorMessage", ex.getMessage());

        model.addAttribute(
                "items",
                fnBItemService.GetAllItems()
        );

        model.addAttribute(
                "item",
                new FnBItemRequest()
        );

        return "manager/fnb-items";
    }
}