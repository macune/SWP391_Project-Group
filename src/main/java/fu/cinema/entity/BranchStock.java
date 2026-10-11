package fu.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "branch_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private FnBItem item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}