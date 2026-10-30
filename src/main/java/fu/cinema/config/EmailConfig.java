package fu.cinema.config;

import fu.cinema.enums.NotificationStatus;
import fu.cinema.enums.NotificationType;
import fu.cinema.repository.NotificationRepository;
import jakarta.annotation.PostConstruct;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@EnableAsync
@EnableScheduling
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    @Builder.Default
    private String host = "smtp.gmail.com";

    @Value("${spring.mail.port:587}")
    @Builder.Default
    private Integer port = 587;

    @Value("${spring.mail.username:trinhhoangduc111@gmail.com}")
    @Builder.Default
    private String username = "trinhhoangduc111@gmail.com";

    @Value("${spring.mail.password:}")
    @Builder.Default
    private String password = "";

    @Builder.Default
    private String senderName = "CineFlow Cinema";

    @Builder.Default
    private Boolean smtpAuth = true;

    @Builder.Default
    private Boolean starttlsEnable = true;

    @Builder.Default
    private Boolean globalEmailEnabled = true;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Danh sách 9 mẫu Template Email & In-App lưu trực tiếp trong Config
    private final List<TemplateItem> templates = new CopyOnWriteArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateItem {
        private Long templateId;
        private String templateCode;
        private String templateName;
        private NotificationType type;
        private String subject;
        private String inAppTemplate;
        private String emailHtmlTemplate;
        @Builder.Default
        private Boolean emailEnabled = true;
        @Builder.Default
        private Boolean active = true;
        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();
    }

    @PostConstruct
    public void initDefaultTemplates() {
        if (!templates.isEmpty()) return;

        addTemplate(1L, "BOOKING_CONFIRMED", "Xác nhận đặt vé thành công (FE-10.1)", NotificationType.BOOKING,
                "[CineFlow] Xác nhận đặt vé thành công - Đơn #{{bookingId}}",
                "Đơn đặt vé #{{bookingId}} cho phim {{movieTitle}} ({{showtime}}) tại {{branchName}} - Ghế: {{seats}} đã được xác nhận thành công.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#e50914;'>🎬 Xác Nhận Đặt Vé Thành Công!</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Cảm ơn bạn đã đặt vé tại <strong>CineFlow Cinema</strong>. Thông tin vé của bạn:</p>"
                        + "<ul>"
                        + "<li><strong>Mã đơn hàng:</strong> #{{bookingId}}</li>"
                        + "<li><strong>Phim:</strong> {{movieTitle}}</li>"
                        + "<li><strong>Rạp / Phòng chiếu:</strong> {{branchName}} - {{hallName}}</li>"
                        + "<li><strong>Suất chiếu:</strong> {{showtime}}</li>"
                        + "<li><strong>Ghế đã đặt:</strong> {{seats}}</li>"
                        + "<li><strong>Tổng thanh toán:</strong> {{totalAmount}}</li>"
                        + "</ul>"
                        + "<p>Vui lòng xuất trình mã QR vé điện tử tại quầy soát vé trước giờ chiếu 15 phút.</p>"
                        + "</div>");

        addTemplate(2L, "BOOKING_CANCELLED", "Thông báo hủy đơn đặt vé (FE-10.1)", NotificationType.BOOKING,
                "[CineFlow] Thông báo hủy đơn đặt vé #{{bookingId}}",
                "Đơn đặt vé #{{bookingId}} (Phim: {{movieTitle}}) của bạn đã được hủy. Ghế {{seats}} đã được giải phóng.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#dc3545;'>Thông Báo Hủy Đơn Đặt Vé #{{bookingId}}</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Đơn đặt vé <strong>#{{bookingId}}</strong> cho bộ phim <strong>{{movieTitle}}</strong> (Suất: {{showtime}}) đã chuyển sang trạng thái <strong>ĐÃ HỦY</strong>.</p>"
                        + "<p>Nếu bạn đã thanh toán trực tuyến, vui lòng liên hệ quầy vé tại chi nhánh {{branchName}} để được hỗ trợ hoàn tiền theo quy định.</p>"
                        + "</div>");

        addTemplate(3L, "BOOKING_STATUS_UPDATED", "Cập nhật trạng thái đơn đặt vé (FE-10.1)", NotificationType.BOOKING,
                "[CineFlow] Cập nhật trạng thái đơn đặt vé #{{bookingId}}",
                "Đơn đặt vé #{{bookingId}} (Phim: {{movieTitle}}) vừa được cập nhật trạng thái thành: {{status}}.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#0d6efd;'>Cập Nhật Trạng Thái Đơn Đặt Vé #{{bookingId}}</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Trạng thái đơn đặt vé <strong>#{{bookingId}}</strong> (Phim: <strong>{{movieTitle}}</strong>) của bạn đã được cập nhật thành: <strong>{{status}}</strong>.</p>"
                        + "</div>");

        addTemplate(4L, "PAYMENT_SUCCESS", "Thông báo thanh toán thành công (FE-10.2)", NotificationType.PAYMENT,
                "[CineFlow] Biên lai thanh toán thành công - Đơn #{{bookingId}}",
                "Thanh toán thành công {{totalAmount}} cho đơn #{{bookingId}} qua {{paymentMethod}} (Mã GD: {{transactionNo}}).",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#198754;'>💳 Thanh Toán Thành Công!</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Hệ thống CineFlow xác nhận đã nhận thanh toán cho đơn hàng <strong>#{{bookingId}}</strong>:</p>"
                        + "<ul>"
                        + "<li><strong>Số tiền:</strong> {{totalAmount}}</li>"
                        + "<li><strong>Phương thức:</strong> {{paymentMethod}}</li>"
                        + "<li><strong>Mã giao dịch:</strong> {{transactionNo}}</li>"
                        + "</ul>"
                        + "</div>");

        addTemplate(5L, "PAYMENT_FAILED", "Thông báo thanh toán thất bại (FE-10.2)", NotificationType.PAYMENT,
                "[CineFlow] Giao dịch thanh toán không thành công - Đơn #{{bookingId}}",
                "Giao dịch thanh toán cho đơn #{{bookingId}} không thành công. Lý do: {{reason}}. Vui lòng thử lại trước khi hết thời gian giữ ghế.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#dc3545;'>⚠️ Thanh Toán Không Thành Công</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Giao dịch thanh toán cho đơn đặt vé <strong>#{{bookingId}}</strong> chưa hoàn tất.</p>"
                        + "<p><strong>Chi tiết:</strong> {{reason}}</p>"
                        + "<p>Vui lòng thực hiện thanh toán lại trong thời gian giữ ghế hoặc đặt lại ghế mới.</p>"
                        + "</div>");

        addTemplate(6L, "SHOWTIME_REMINDER", "Nhắc nhở giờ chiếu trước 2 tiếng (FE-10.3 - JOB-03)", NotificationType.SHOWTIME,
                "[CineFlow] Nhắc lịch chiếu: Phim {{movieTitle}} sắp bắt đầu!",
                "Nhắc hẹn: Suất chiếu phim {{movieTitle}} của bạn tại {{branchName}} ({{hallName}}) sẽ bắt đầu lúc {{showtime}}. Ghế: {{seats}}.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#e50914;'>⏰ Nhắc Hẹn Giờ Chiếu Phim!</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Chỉ còn chưa đầy 2 giờ nữa là suất chiếu bộ phim <strong>{{movieTitle}}</strong> của bạn sẽ bắt đầu!</p>"
                        + "<ul>"
                        + "<li><strong>Rạp:</strong> {{branchName}} - {{hallName}}</li>"
                        + "<li><strong>Giờ chiếu:</strong> {{showtime}}</li>"
                        + "<li><strong>Ghế của bạn:</strong> {{seats}}</li>"
                        + "</ul>"
                        + "<p>Chúc bạn có những giây phút thưởng thức điện ảnh tuyệt vời tại CineFlow!</p>"
                        + "</div>");

        addTemplate(7L, "SHOWTIME_CHANGED", "Thông báo thay đổi lịch chiếu (FE-10.3)", NotificationType.SHOWTIME,
                "[CineFlow] Thông báo thay đổi lịch chiếu phim {{movieTitle}}",
                "Lịch chiếu phim {{movieTitle}} (Đơn #{{bookingId}}) đã thay đổi từ [{{oldSchedule}}] sang [{{showtime}} - {{hallName}}] tại {{branchName}}.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#fd7e14;'>📢 Thông Báo Thay Đổi Lịch Chiếu</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Suất chiếu bộ phim <strong>{{movieTitle}}</strong> thuộc đơn đặt vé <strong>#{{bookingId}}</strong> của bạn vừa được điều chỉnh:</p>"
                        + "<ul>"
                        + "<li><strong>Lịch cũ:</strong> {{oldSchedule}}</li>"
                        + "<li><strong>Lịch mới:</strong> {{showtime}} tại {{branchName}} ({{hallName}})</li>"
                        + "</ul>"
                        + "<p>Rất mong quý khách thông cảm vì sự bất tiện này.</p>"
                        + "</div>");

        addTemplate(8L, "SHOWTIME_CANCELLED", "Thông báo hủy suất chiếu (FE-10.3)", NotificationType.SHOWTIME,
                "[CineFlow] Thông báo hủy suất chiếu phim {{movieTitle}}",
                "Suất chiếu phim {{movieTitle}} lúc {{showtime}} tại {{branchName}} ({{hallName}}) đã bị hủy vì lý do vận hành. Đơn #{{bookingId}} sẽ được hoàn tiền 100%.",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#dc3545;'>📢 Thông Báo Hủy Suất Chiếu</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Chúng tôi rất tiếc phải thông báo suất chiếu phim <strong>{{movieTitle}}</strong> lúc <strong>{{showtime}}</strong> tại <strong>{{branchName}} ({{hallName}})</strong> đã bị hủy do sự cố vận hành.</p>"
                        + "<p>Đơn đặt vé <strong>#{{bookingId}}</strong> của quý khách đã được chuyển sang trạng thái Hủy và được hỗ trợ hoàn tiền 100% tại quầy dịch vụ.</p>"
                        + "</div>");

        addTemplate(9L, "PROMOTION_ALERT", "Thông báo Khuyến mãi & Mã giảm giá mới (FE-10.4)", NotificationType.PROMOTION,
                "[CineFlow] 🎁 Ưu đãi mới: Nhập mã {{promoCode}} giảm ngay {{discountInfo}}!",
                "Ưu đãi hot từ CineFlow: Sử dụng mã giảm giá [{{promoCode}}] để được giảm {{discountInfo}} (Áp dụng từ {{validFrom}} đến {{expiryDate}}).",
                "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                        + "<h2 style='color:#e50914;'>🎁 Ưu Đãi Đặc Biệt Dành Cho Thành Viên CineFlow!</h2>"
                        + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                        + "<p>Đừng bỏ lỡ chương trình khuyến mãi mới nhất tại rạp CineFlow:</p>"
                        + "<div style='background:#fff3cd;padding:15px;border-left:4px solid #e50914;margin:15px 0;'>"
                        + "<h3 style='margin:0;color:#d63384;'>MÃ COUPON: {{promoCode}}</h3>"
                        + "<p style='margin:5px 0 0;'>Mức giảm: <strong>{{discountInfo}}</strong></p>"
                        + "<p style='margin:5px 0 0;'>Thời hạn: <strong>{{validFrom}}</strong> đến <strong>{{expiryDate}}</strong></p>"
                        + "</div>"
                        + "<p>Hãy đặt vé ngay hôm nay để tận hưởng ưu đãi!</p>"
                        + "</div>");
    }

    private void addTemplate(Long id, String code, String name, NotificationType type,
                             String subject, String inApp, String html) {
        templates.add(TemplateItem.builder()
                .templateId(id)
                .templateCode(code)
                .templateName(name)
                .type(type)
                .subject(subject)
                .inAppTemplate(inApp)
                .emailHtmlTemplate(html)
                .emailEnabled(true)
                .active(true)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    public Optional<TemplateItem> findTemplateByCode(String code) {
        return templates.stream()
                .filter(t -> t.getTemplateCode().equalsIgnoreCase(code))
                .findFirst();
    }

    public Optional<TemplateItem> findTemplateById(Long id) {
        return templates.stream()
                .filter(t -> t.getTemplateId().equals(id))
                .findFirst();
    }

    // Tự động đếm số thông báo chưa đọc hiển thị lên biểu tượng Chuông ở Header
    @ControllerAdvice
    @RequiredArgsConstructor
    public static class NotificationHeaderAdvice {

        private final NotificationRepository notificationRepository;

        @ModelAttribute("unreadNotificationCount")
        public long populateUnreadNotificationCount() {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                    return notificationRepository.countByRecipient_UsernameAndIsReadFalseAndStatus(
                            auth.getName(), NotificationStatus.SENT);
                }
            } catch (Exception ignored) {
            }
            return 0L;
        }
    }
}