package fu.cinema.repository;

import fu.cinema.entity.FnBItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FnBItemRepository extends JpaRepository<FnBItem,Long> {
}
