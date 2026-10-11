package fu.cinema.repository;

import fu.cinema.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> { // Trả về Long

    List<Promotion> findByIsActiveTrueAndExpiryDateAfter(LocalDateTime now);


    Optional<Promotion> findByCode(String code);
}