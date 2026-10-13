package fu.cinema.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchStockResponse {
    private Long id;
    private Long branchId;
    private String branchName;
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private Integer quantity;
    private LocalDateTime lastUpdated;

    private BigDecimal price;
    private String imageUrl;
    private Boolean isAvailable;
}