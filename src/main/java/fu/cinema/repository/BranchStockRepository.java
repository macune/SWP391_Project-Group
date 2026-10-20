package fu.cinema.repository;

import fu.cinema.entity.BranchStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {

    List<BranchStock> findByBranch_BranchId(Long branchId);

    Optional<BranchStock> findByBranch_BranchIdAndItem_ItemId(Long branchId, Long itemId);
}