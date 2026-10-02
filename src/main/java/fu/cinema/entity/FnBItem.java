package fu.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fnb_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnBItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "fnbItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingFnB> bookingFnbs = new ArrayList<>();
}
