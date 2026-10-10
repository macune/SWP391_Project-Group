package fu.cinema.repository;

import fu.cinema.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByBranchBranchId(Long branchId);
    Optional<Staff> findByAccount_Username(String username);
}
