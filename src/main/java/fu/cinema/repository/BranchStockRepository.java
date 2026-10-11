package fu.cinema.repository;

import fu.cinema.entity.BranchStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {


    @Query("SELECT bs FROM BranchStock bs WHERE bs.branch.branchId = :branchId")
    List<BranchStock> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT bs FROM BranchStock bs WHERE bs.branch.branchId = :branchId AND bs.item.itemId = :itemId")
    Optional<BranchStock> findByBranchIdAndItemId(@Param("branchId") Long branchId, @Param("itemId") Long itemId);
}