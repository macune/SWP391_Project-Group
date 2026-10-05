package fu.cinema.repository;

import fu.cinema.entity.FnBItem;
import fu.cinema.enums.FnBCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnBItemRepository extends JpaRepository<FnBItem,Long> {
boolean existsByNameIgnoreCase(String name);
List<FnBItem> findByIsAvailable(Boolean isAvailable);
    @Query("""
    SELECT f
    FROM FnBItem f
    WHERE (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%')))
      AND (:category IS NULL OR f.category = :category)
""")
    List<FnBItem> searchItems(
            @Param("name") String name,
            @Param("category") FnBCategory category
    );
}
