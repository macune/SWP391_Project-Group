package fu.cinema.repository;

import fu.cinema.entity.Account;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByEmail(String email);

    @EntityGraph(attributePaths = {"customer", "staff"})
    List<Account> findByRole(Role role);

    @EntityGraph(attributePaths = {"customer", "staff"})
    List<Account> findByRoleAndStatusNotAndStaff_BranchIsNull(Role role, AccountStatus status);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<Account> findByEmailAndStatus(String email, AccountStatus status);

    @EntityGraph(attributePaths = {"customer", "staff"})
    List<Account> findByStatusIn(Collection<AccountStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "staff"})
    List<Account> findByUsernameContainingIgnoreCaseAndStatusIn(String username, Collection<AccountStatus> statuses);
}