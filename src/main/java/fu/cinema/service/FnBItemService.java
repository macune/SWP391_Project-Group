package fu.cinema.service;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.enums.FnBCategory;

import java.util.List;

public interface FnBItemService {
    List<FnBItemResponse> GetAllItems();
    FnBItemResponse GetItemById(Long id);
    void createItem(FnBItemRequest fnBItemRequest);
    void updateItem(Long id, FnBItemRequest fnBItemRequest);
    void deleteItem(Long id);

    List<FnBItemResponse> searchItems(
            String name,
            FnBCategory category
    );
}
