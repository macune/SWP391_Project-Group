package fu.cinema.entity;

import fu.cinema.enums.PromotionStatus; // NHỚ IMPORT ENUM NÀY VÀO
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long id;

    @Column(length = 50, unique = true)
    private String code;

    @Column(name = "discount_percentage", nullable = false)
    private Integer discountPercentage;

    @Column(name = "discount_type", nullable = false)
    @Builder.Default
    private String discountType = "PERCENTAGE";

    @Column(name = "max_discount", nullable = false)
    private BigDecimal maxDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime expiryDate;

    // SỬ DỤNG ENUM THAY VÌ STRING
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.ACTIVE;

    @Transient
    private Boolean isActive;

    @PrePersist
    @PreUpdate
    public void checkDefaultValues() {
        if (this.maxDiscountAmount == null) {
            this.maxDiscountAmount = BigDecimal.ZERO;
        }
        if (this.status == null) {
            this.status = (this.isActive != null && this.isActive) ? PromotionStatus.ACTIVE : PromotionStatus.DISABLED;
        }
        if (this.validFrom == null) {
            this.validFrom = LocalDateTime.now();
        }
    }
}