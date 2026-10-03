package fu.cinema.mapper;

import fu.cinema.dto.request.FnBItemRequest;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.entity.FnBItem;
import org.springframework.stereotype.Component;


@Component
public class FnBItemMapper {
    public FnBItem toEntity(FnBItemRequest fnBItemRequest) {
        return FnBItem.builder()
                .name(fnBItemRequest.getName())
                .category(fnBItemRequest.getCategory())
                .price(fnBItemRequest.getPrice())
                .isAvailable(
                        fnBItemRequest.getIsAvailable() != null
                                ? fnBItemRequest.getIsAvailable()
                                : true
                )
                .imageUrl(fnBItemRequest.getImageUrl())
                .build();
    }

    public FnBItemResponse toResponse(FnBItem fnBItem) {
        return FnBItemResponse.builder()
                .id(fnBItem.getItemId())
                .name(fnBItem.getName())
                .category(fnBItem.getCategory())
                .price(fnBItem.getPrice())
                .isAvailable(fnBItem.getIsAvailable())
                .imageUrl(fnBItem.getImageUrl())
                .build();
    }

    public void update(FnBItem fnBItem, FnBItemRequest fnBItemRequest) {
        fnBItem.setName(fnBItemRequest.getName());
        fnBItem.setCategory(fnBItemRequest.getCategory());
        fnBItem.setPrice(fnBItemRequest.getPrice());
        if (fnBItemRequest.getIsAvailable() != null) {
            fnBItem.setIsAvailable(fnBItemRequest.getIsAvailable());
        }
        fnBItem.setImageUrl(fnBItemRequest.getImageUrl());
    }
}
