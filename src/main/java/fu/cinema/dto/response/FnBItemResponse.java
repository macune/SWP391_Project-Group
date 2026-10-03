package fu.cinema.dto.response;

import fu.cinema.enums.FnBCategory;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FnBItemResponse {
private  Long id;
private String name;
private FnBCategory category;
private BigDecimal price;
private Boolean isAvailable;
private String imageUrl;

}
