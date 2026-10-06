package fu.cinema.entity;

import fu.cinema.enums.BranchStatus;
import fu.cinema.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_name", nullable = false, unique = true, columnDefinition = "NVARCHAR(255)")
    private String branchName;

    @Column(name = "address", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String address;

    @Column(name = "hotline", nullable = false, length = 20)
    private String hotline;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private BranchStatus status = BranchStatus.ACTIVE;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Hall> halls = new ArrayList<>();

    @OneToMany(mappedBy = "branch", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Staff> staffList = new ArrayList<>();

    // Lấy Quản lý duy nhất đang phụ trách chi nhánh
    @Transient
    public Staff getManager() {
        if (staffList == null || staffList.isEmpty()) {
            return null;
        }
        return staffList.stream()
                .filter(s -> s.getAccount() != null && s.getAccount().getRole() == Role.MANAGER)
                .findFirst()
                .orElse(null);
    }
}