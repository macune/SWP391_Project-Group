package fu.cinema.repository;

import fu.cinema.entity.Staff;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByBranchBranchId(Long branchId);
    Optional<Staff> findByPhone(String phone);

    @EntityGraph(attributePaths = {"account", "branch"})
    List<Staff> findByAccount_RoleAndAccount_StatusNot(Role role, AccountStatus status);

    @EntityGraph(attributePaths = {"account", "branch"})
    List<Staff> findByBranch_BranchIdAndAccount_RoleAndAccount_StatusNot(Long branchId, Role role, AccountStatus status);

    boolean existsByAccount_RoleAndBranch_BranchIdAndAccount_StatusNot(Role role, Long branchId, AccountStatus status);
}
