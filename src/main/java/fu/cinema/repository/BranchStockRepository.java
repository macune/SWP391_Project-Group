package fu.cinema.repository;

import fu.cinema.entity.BranchStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {

    // Lấy toàn bộ danh sách tồn kho của một chi nhánh cụ thể
    List<BranchStock> findByBranch_BranchId(Long branchId);

    // Tìm chính xác số lượng tồn của 1 món tại 1 chi nhánh
    Optional<BranchStock> findByBranch_BranchIdAndFnbItem_ItemId(Long branchId, Long itemId);
}