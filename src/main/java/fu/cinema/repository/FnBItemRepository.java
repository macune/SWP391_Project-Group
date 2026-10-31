package fu.cinema.repository;

import fu.cinema.entity.FnBItem;
import fu.cinema.enums.FnBCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnBItemRepository extends JpaRepository<FnBItem, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<FnBItem> findByIsAvailable(Boolean isAvailable);

    // 1. Tìm theo cả Tên (có chứa từ khóa, không phân biệt hoa thường) VÀ Danh mục
    List<FnBItem> findByNameContainingIgnoreCaseAndCategory(String name, FnBCategory category);

    // 2. Chỉ tìm theo Tên
    List<FnBItem> findByNameContainingIgnoreCase(String name);

    // 3. Chỉ tìm theo Danh mục
    List<FnBItem> findByCategory(FnBCategory category);
}