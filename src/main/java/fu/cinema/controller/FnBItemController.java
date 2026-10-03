package fu.cinema.controller;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.service.FnBItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fnb-items")
@RequiredArgsConstructor
public class FnBItemController {

    private final FnBItemService fnBItemService;

    @GetMapping
    public ResponseEntity<List<FnBItemResponse>> getAllItems() {

        return ResponseEntity.ok(
                fnBItemService.GetAllItems()
        );
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<FnBItemResponse> getItemById(
            @PathVariable Long itemId) {

        return ResponseEntity.ok(
                fnBItemService.GetItemById(itemId)
        );
    }

    @PostMapping
    public ResponseEntity<FnBItemResponse> createItem(
            @Valid @RequestBody FnBItemRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fnBItemService.createItem(request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<FnBItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody FnBItemRequest request) {

        return ResponseEntity.ok(
                fnBItemService.updateItem(
                        itemId,
                        request
                )
        );
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId) {

        fnBItemService.deleteItem(itemId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{itemId}/availability")
    public ResponseEntity<FnBItemResponse> changeAvailability(
            @PathVariable Long itemId,
            @RequestParam Boolean isAvailable) {

        return ResponseEntity.ok(
                fnBItemService.changeAvailability(
                        itemId,
                        isAvailable
                )
        );
    }
}