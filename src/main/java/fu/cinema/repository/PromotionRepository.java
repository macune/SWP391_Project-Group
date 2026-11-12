package fu.cinema.repository;

import fu.cinema.entity.Promotion;
import fu.cinema.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    //Lấy danh sách mã có chứa từ khóa
    List<Promotion> findByCodeContainingIgnoreCase(String code);

    //Lấy chính xác 1 mã khuyến mãi
    Optional<Promotion> findByCode(String code);

    // Trả về true nếu mã code đã tồn tại (không phân biệt hoa/thường)
    boolean existsByCodeIgnoreCase(String code);

    // Lấy danh sách mã theo trạng thái (ACTIVE) và vẫn còn hạn sử dụng
    List<Promotion> findByStatusAndExpiryDateAfter(PromotionStatus promotionStatus, LocalDateTime now);
}