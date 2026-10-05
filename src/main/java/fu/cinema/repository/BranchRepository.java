package fu.cinema.repository;

import fu.cinema.entity.Branch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByBranchName(String branchName);

    @Override
    @EntityGraph(attributePaths = {"staffList", "staffList.account", "staffList.account.customer"})
    List<Branch> findAll();
}
