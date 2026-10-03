package fu.cinema.repository;

import fu.cinema.entity.FnBItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FnBItemRepository extends JpaRepository<FnBItem,Long> {
boolean existsByNameIgnoreCase(String name);
List<FnBItem> findByIsAvailable(Boolean isAvailable);

}
