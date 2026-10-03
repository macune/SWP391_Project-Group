package fu.cinema.dto.request;

import fu.cinema.enums.FnBCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FnBItemRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "category is required")
    private FnBCategory category;
    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Availability is required")
    private Boolean isAvailable;

    private String imageUrl;
}
