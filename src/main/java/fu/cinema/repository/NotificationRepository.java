package fu.cinema.repository;

import fu.cinema.entity.Notification;
import fu.cinema.enums.NotificationStatus;
import fu.cinema.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy danh sách thông báo đã gửi của 1 khách hàng (mới nhất lên đầu)
    List<Notification> findByRecipient_UsernameAndStatusOrderByCreatedAtDesc(String username, NotificationStatus status);

    // Lọc thông báo của khách hàng theo loại
    List<Notification> findByRecipient_UsernameAndTypeAndStatusOrderByCreatedAtDesc(
            String username, NotificationType type, NotificationStatus status);

    // Đếm số thông báo chưa đọc của khách hàng để hiển thị trên biểu tượng Chuông ở Header
    long countByRecipient_UsernameAndIsReadFalseAndStatus(String username, NotificationStatus status);

    // Kiểm tra xem thông báo nhắc lịch chiếu (JOB-03) hoặc khuyến mãi đã được tạo cho đơn/mã này chưa
    boolean existsByRecipient_AccountIdAndReferenceIdAndTemplateCode(
            Long accountId, Long referenceId, String templateCode);

    // Phục vụ JOB-04: Tìm tất cả thông báo hẹn giờ (SCHEDULED) đã đến thời điểm gửi
    List<Notification> findByStatusAndScheduledAtLessThanEqual(NotificationStatus status, LocalDateTime now);

    // Phục vụ Admin (FE-10.5): Xem toàn bộ lịch sử thông báo
    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    long countByStatus(NotificationStatus status);
}