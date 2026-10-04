package fu.cinema.repository;

import fu.cinema.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {
    List<Hall> findByBranchBranchId(Long branchId);
    boolean existsByBranchBranchIdAndHallName(Long branchId, String hallName);
}
