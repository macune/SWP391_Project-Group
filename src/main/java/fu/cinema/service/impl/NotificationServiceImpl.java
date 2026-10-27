package fu.cinema.service.impl;

import fu.cinema.entity.*;
import fu.cinema.enums.*;
import fu.cinema.repository.*;
import fu.cinema.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailConfigRepository emailConfigRepository;
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final PromotionRepository promotionRepository;
    private final Environment environment;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    // Khởi tạo dữ liệu mặc định cho Cấu hình SMTP và 9 Mẫu Thông báo/Email khi hệ thống chạy lần đầu
    @PostConstruct
    @Transactional
    public void initDefaultTemplatesAndConfig() {
        try {
            if (emailConfigRepository.count() == 0) {
                String defaultUser = environment.getProperty("spring.mail.username", "trinhhoangduc111@gmail.com");
                String defaultPass = environment.getProperty("spring.mail.password", "bcvl lzgy vcek oxjv");
                EmailConfig config = EmailConfig.builder()
                        .host(environment.getProperty("spring.mail.host", "smtp.gmail.com"))
                        .port(Integer.parseInt(environment.getProperty("spring.mail.port", "587")))
                        .username(defaultUser)
                        .password(defaultPass)
                        .senderName("CineFlow Cinema")
                        .smtpAuth(true)
                        .starttlsEnable(true)
                        .globalEmailEnabled(true)
                        .updatedAt(LocalDateTime.now())
                        .build();
                emailConfigRepository.save(config);
            }

            seedTemplateIfMissing("BOOKING_CONFIRMED", "Xác nhận đặt vé thành công (FE-10.1)", NotificationType.BOOKING,
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

            seedTemplateIfMissing("BOOKING_CANCELLED", "Thông báo hủy đơn đặt vé (FE-10.1)", NotificationType.BOOKING,
                    "[CineFlow] Thông báo hủy đơn đặt vé #{{bookingId}}",
                    "Đơn đặt vé #{{bookingId}} (Phim: {{movieTitle}}) của bạn đã được hủy. Ghế {{seats}} đã được giải phóng.",
                    "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                            + "<h2 style='color:#dc3545;'>Thông Báo Hủy Đơn Đặt Vé #{{bookingId}}</h2>"
                            + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                            + "<p>Đơn đặt vé <strong>#{{bookingId}}</strong> cho bộ phim <strong>{{movieTitle}}</strong> (Suất: {{showtime}}) đã chuyển sang trạng thái <strong>ĐÃ HỦY</strong>.</p>"
                            + "<p>Nếu bạn đã thanh toán trực tuyến, vui lòng liên hệ quầy vé tại chi nhánh {{branchName}} để được hỗ trợ hoàn tiền theo quy định.</p>"
                            + "</div>");

            seedTemplateIfMissing("BOOKING_STATUS_UPDATED", "Cập nhật trạng thái đơn đặt vé (FE-10.1)", NotificationType.BOOKING,
                    "[CineFlow] Cập nhật trạng thái đơn đặt vé #{{bookingId}}",
                    "Đơn đặt vé #{{bookingId}} (Phim: {{movieTitle}}) vừa được cập nhật trạng thái thành: {{status}}.",
                    "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                            + "<h2 style='color:#0d6efd;'>Cập Nhật Trạng Thái Đơn Đặt Vé #{{bookingId}}</h2>"
                            + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                            + "<p>Trạng thái đơn đặt vé <strong>#{{bookingId}}</strong> (Phim: <strong>{{movieTitle}}</strong>) của bạn đã được cập nhật thành: <strong>{{status}}</strong>.</p>"
                            + "</div>");

            seedTemplateIfMissing("PAYMENT_SUCCESS", "Thông báo thanh toán thành công (FE-10.2)", NotificationType.PAYMENT,
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

            seedTemplateIfMissing("PAYMENT_FAILED", "Thông báo thanh toán thất bại (FE-10.2)", NotificationType.PAYMENT,
                    "[CineFlow] Giao dịch thanh toán không thành công - Đơn #{{bookingId}}",
                    "Giao dịch thanh toán cho đơn #{{bookingId}} không thành công. Lý do: {{reason}}. Vui lòng thử lại trước khi hết thời gian giữ ghế.",
                    "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                            + "<h2 style='color:#dc3545;'>⚠️ Thanh Toán Không Thành Công</h2>"
                            + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                            + "<p>Giao dịch thanh toán cho đơn đặt vé <strong>#{{bookingId}}</strong> chưa hoàn tất.</p>"
                            + "<p><strong>Chi tiết:</strong> {{reason}}</p>"
                            + "<p>Vui lòng thực hiện thanh toán lại trong thời gian giữ ghế hoặc đặt lại ghế mới.</p>"
                            + "</div>");

            seedTemplateIfMissing("SHOWTIME_REMINDER", "Nhắc nhở giờ chiếu trước 2 tiếng (FE-10.3 - JOB-03)", NotificationType.SHOWTIME,
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

            seedTemplateIfMissing("SHOWTIME_CHANGED", "Thông báo thay đổi lịch chiếu (FE-10.3)", NotificationType.SHOWTIME,
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

            seedTemplateIfMissing("SHOWTIME_CANCELLED", "Thông báo hủy suất chiếu (FE-10.3)", NotificationType.SHOWTIME,
                    "[CineFlow] Thông báo hủy suất chiếu phim {{movieTitle}}",
                    "Suất chiếu phim {{movieTitle}} lúc {{showtime}} tại {{branchName}} ({{hallName}}) đã bị hủy vì lý do vận hành. Đơn #{{bookingId}} sẽ được hoàn tiền 100%.",
                    "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                            + "<h2 style='color:#dc3545;'>📢 Thông Báo Hủy Suất Chiếu</h2>"
                            + "<p>Xin chào <strong>{{customerName}}</strong>,</p>"
                            + "<p>Chúng tôi rất tiếc phải thông báo suất chiếu phim <strong>{{movieTitle}}</strong> lúc <strong>{{showtime}}</strong> tại <strong>{{branchName}} ({{hallName}})</strong> đã bị hủy do sự cố vận hành.</p>"
                            + "<p>Đơn đặt vé <strong>#{{bookingId}}</strong> của quý khách đã được chuyển sang trạng thái Hủy và được hỗ trợ hoàn tiền 100% tại quầy dịch vụ.</p>"
                            + "</div>");

            seedTemplateIfMissing("PROMOTION_ALERT", "Thông báo Khuyến mãi & Mã giảm giá mới (FE-10.4)", NotificationType.PROMOTION,
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
        } catch (Exception e) {
            log.warn("Không thể khởi tạo template thông báo mặc định: {}", e.getMessage());
        }
    }

    private void seedTemplateIfMissing(String code, String name, NotificationType type,
                                       String subject, String inApp, String html) {
        if (templateRepository.findByTemplateCode(code).isEmpty()) {
            templateRepository.save(NotificationTemplate.builder()
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
    }

    // =========================================================================
    // FE-10.1: BOOKING NOTIFICATIONS
    // =========================================================================
    @Override
    @Transactional
    public void sendBookingConfirmedAlert(Booking booking) {
        if (booking == null || booking.getAccount() == null) return;
        Map<String, String> vars = buildBookingVariables(booking);
        createAndDispatchFromTemplate(booking.getAccount(), "BOOKING_CONFIRMED",
                NotificationType.BOOKING, booking.getBookingId(), vars, null);
    }

    @Override
    @Transactional
    public void sendBookingCancelledAlert(Booking booking) {
        if (booking == null || booking.getAccount() == null) return;
        Map<String, String> vars = buildBookingVariables(booking);
        createAndDispatchFromTemplate(booking.getAccount(), "BOOKING_CANCELLED",
                NotificationType.BOOKING, booking.getBookingId(), vars, null);
    }

    @Override
    @Transactional
    public void sendBookingStatusUpdatedAlert(Booking booking) {
        if (booking == null || booking.getAccount() == null) return;
        Map<String, String> vars = buildBookingVariables(booking);
        vars.put("status", booking.getStatus() != null ? booking.getStatus().name() : "UPDATED");
        createAndDispatchFromTemplate(booking.getAccount(), "BOOKING_STATUS_UPDATED",
                NotificationType.BOOKING, booking.getBookingId(), vars, null);
    }

    // =========================================================================
    // FE-10.2: PAYMENT NOTIFICATIONS
    // =========================================================================
    @Override
    @Transactional
    public void sendPaymentSuccessAlert(Booking booking, PaymentMethod method, String transactionNo) {
        if (booking == null || booking.getAccount() == null) return;
        Map<String, String> vars = buildBookingVariables(booking);
        vars.put("paymentMethod", method != null ? method.name() : "ONLINE");
        vars.put("transactionNo", transactionNo != null ? transactionNo : "N/A");
        createAndDispatchFromTemplate(booking.getAccount(), "PAYMENT_SUCCESS",
                NotificationType.PAYMENT, booking.getBookingId(), vars, null);
    }

    @Override
    @Transactional
    public void sendPaymentFailedAlert(Long bookingId, String reason) {
        if (bookingId == null) return;
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            if (booking.getAccount() != null) {
                Map<String, String> vars = buildBookingVariables(booking);
                vars.put("reason", reason != null ? reason : "Giao dịch bị hủy hoặc hết hạn thanh toán.");
                createAndDispatchFromTemplate(booking.getAccount(), "PAYMENT_FAILED",
                        NotificationType.PAYMENT, booking.getBookingId(), vars, null);
            }
        });
    }

    // =========================================================================
    // FE-10.3: SHOWTIME NOTIFICATIONS & JOB-03 (REMINDERS)
    // =========================================================================
    @Override
    @Transactional
    public void sendShowtimeChangedAlert(Showtime showtime, String oldScheduleInfo, List<Booking> affectedBookings) {
        if (showtime == null || affectedBookings == null || affectedBookings.isEmpty()) return;
        for (Booking booking : affectedBookings) {
            if (booking.getAccount() == null) continue;
            Map<String, String> vars = buildBookingVariables(booking);
            vars.put("oldSchedule", oldScheduleInfo);
            vars.put("showtime", showtime.getStartTime().format(DATE_TIME_FMT));
            vars.put("hallName", showtime.getHall().getHallName());
            vars.put("branchName", showtime.getHall().getBranch().getBranchName());
            vars.put("movieTitle", showtime.getMovie().getTitle());
            createAndDispatchFromTemplate(booking.getAccount(), "SHOWTIME_CHANGED",
                    NotificationType.SHOWTIME, showtime.getShowtimeId(), vars, null);
        }
    }

    @Override
    @Transactional
    public void sendShowtimeCancelledAlert(Showtime showtime, List<Booking> affectedBookings) {
        if (showtime == null || affectedBookings == null || affectedBookings.isEmpty()) return;
        for (Booking booking : affectedBookings) {
            if (booking.getAccount() == null) continue;
            Map<String, String> vars = buildBookingVariables(booking);
            vars.put("showtime", showtime.getStartTime().format(DATE_TIME_FMT));
            vars.put("hallName", showtime.getHall().getHallName());
            vars.put("branchName", showtime.getHall().getBranch().getBranchName());
            vars.put("movieTitle", showtime.getMovie().getTitle());
            createAndDispatchFromTemplate(booking.getAccount(), "SHOWTIME_CANCELLED",
                    NotificationType.SHOWTIME, showtime.getShowtimeId(), vars, null);
        }
    }

    /**
     * JOB-03: Send Showtime Reminders (Chạy định kỳ 5 phút = 300.000ms)
     * Quét tất cả các đơn đặt vé CONFIRMED có suất chiếu bắt đầu trong vòng 2 giờ tới
     */
    @Override
    @Scheduled(fixedRate = 300000)
    @Transactional
    public int processShowtimeRemindersJob() {
        int count = 0;
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextTwoHours = now.plusHours(2);

            List<Booking> upcomingBookings = bookingRepository.findConfirmedBookingsWithShowtimeBetween(
                    BookingStatus.CONFIRMED, now, nextTwoHours);

            if (upcomingBookings.isEmpty()) {
                return 0;
            }

            for (Booking booking : upcomingBookings) {
                Account recipient = booking.getAccount();
                if (recipient == null) continue;

                // Kiểm tra xem đơn này đã được gửi nhắc lịch chiếu chưa (Chống gửi lặp)
                boolean alreadyReminded = notificationRepository.existsByRecipient_AccountIdAndReferenceIdAndTemplateCode(
                        recipient.getAccountId(), booking.getBookingId(), "SHOWTIME_REMINDER");

                if (!alreadyReminded) {
                    Map<String, String> vars = buildBookingVariables(booking);
                    createAndDispatchFromTemplate(recipient, "SHOWTIME_REMINDER",
                            NotificationType.SHOWTIME, booking.getBookingId(), vars, null);
                    count++;
                }
            }
            if (count > 0) {
                log.info("[JOB-03] Đã gửi {} thông báo nhắc lịch chiếu trước 2 giờ.", count);
            }
        } catch (Exception e) {
            log.error("[JOB-03] Lỗi khi quét nhắc lịch chiếu: {}", e.getMessage(), e);
        }
        return count;
    }

    // =========================================================================
    // FE-10.4: PROMOTION NOTIFICATIONS & JOB-04 (SCHEDULED NOTIFICATIONS)
    // =========================================================================
    @Override
    @Transactional
    public void handleNewPromotionNotification(Promotion promotion) {
        if (promotion == null || promotion.getStatus() != PromotionStatus.ACTIVE) return;
        LocalDateTime scheduledTime = (promotion.getValidFrom() != null && promotion.getValidFrom().isAfter(LocalDateTime.now()))
                ? promotion.getValidFrom()
                : null;
        broadcastPromotionAlert(promotion.getId(), scheduledTime);
    }

    @Override
    @Transactional
    public int broadcastPromotionAlert(Long promotionId, LocalDateTime scheduledAt) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình khuyến mãi!"));

        List<Account> activeAccounts = accountRepository.findByStatusIn(List.of(AccountStatus.ACTIVE));
        int count = 0;

        String discountInfo = "FIXED_AMOUNT".equalsIgnoreCase(promo.getDiscountType())
                ? formatCurrency(BigDecimal.valueOf(promo.getDiscountPercentage()))
                : promo.getDiscountPercentage() + "% (Tối đa " + formatCurrency(promo.getMaxDiscountAmount()) + ")";

        for (Account acc : activeAccounts) {
            Map<String, String> vars = new HashMap<>();
            vars.put("customerName", getDisplayName(acc));
            vars.put("promoCode", promo.getCode());
            vars.put("discountInfo", discountInfo);
            vars.put("validFrom", promo.getValidFrom() != null ? promo.getValidFrom().format(DATE_TIME_FMT) : "Ngay bây giờ");
            vars.put("expiryDate", promo.getExpiryDate() != null ? promo.getExpiryDate().format(DATE_TIME_FMT) : "---");

            createAndDispatchFromTemplate(acc, "PROMOTION_ALERT",
                    NotificationType.PROMOTION, promo.getId(), vars, scheduledAt);
            count++;
        }
        return count;
    }

    @Override
    @Transactional
    public int createCustomNotification(String recipientUsername, NotificationType type, String title,
                                        String message, boolean sendEmail, LocalDateTime scheduledAt) {
        List<Account> targets;
        if (recipientUsername == null || recipientUsername.isBlank() || "ALL".equalsIgnoreCase(recipientUsername.trim())) {
            targets = accountRepository.findByStatusIn(List.of(AccountStatus.ACTIVE));
        } else {
            Account acc = accountRepository.findByUsername(recipientUsername.trim())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + recipientUsername));
            targets = List.of(acc);
        }

        boolean isScheduled = scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
        int count = 0;

        for (Account acc : targets) {
            String htmlBody = "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                    + "<h2 style='color:#e50914;'>" + title + "</h2>"
                    + "<p>Xin chào <strong>" + getDisplayName(acc) + "</strong>,</p>"
                    + "<p>" + message.replace("\n", "<br/>") + "</p>"
                    + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'/>"
                    + "<small style='color:#888;'>Email được gửi từ Hệ thống Quản trị Rạp chiếu phim CineFlow.</small>"
                    + "</div>";

            Notification notification = Notification.builder()
                    .recipient(acc)
                    .recipientEmail(acc.getEmail())
                    .title(title)
                    .message(message)
                    .emailContent(sendEmail ? htmlBody : null)
                    .type(type != null ? type : NotificationType.PROMOTION)
                    .templateCode("CUSTOM_MANUAL")
                    .status(isScheduled ? NotificationStatus.SCHEDULED : NotificationStatus.SENT)
                    .isRead(false)
                    .emailSent(false)
                    .scheduledAt(scheduledAt)
                    .sentAt(isScheduled ? null : LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            Notification saved = notificationRepository.save(notification);
            if (!isScheduled && sendEmail) {
                sendEmailAsync(saved.getNotificationId(), acc.getEmail(), title, htmlBody);
            }
            count++;
        }
        return count;
    }

    /**
     * JOB-04: Process Scheduled Notifications (Chạy định kỳ 5 phút = 300.000ms)
     * Quét tất cả thông báo có trạng thái SCHEDULED đã đến hạn gửi (scheduledAt <= now)
     */
    @Override
    @Scheduled(fixedRate = 300000)
    @Transactional
    public int processScheduledNotificationsJob() {
        int processedCount = 0;
        try {
            List<Notification> dueNotifications = notificationRepository.findByStatusAndScheduledAtLessThanEqual(
                    NotificationStatus.SCHEDULED, LocalDateTime.now());

            if (dueNotifications.isEmpty()) {
                return 0;
            }

            for (Notification n : dueNotifications) {
                try {
                    if (n.getRecipient() == null) {
                        n.setStatus(NotificationStatus.FAILED);
                        n.setErrorMessage("Người nhận không hợp lệ");
                        notificationRepository.save(n);
                        continue;
                    }

                    n.setStatus(NotificationStatus.SENT);
                    n.setSentAt(LocalDateTime.now());
                    notificationRepository.save(n);

                    if (n.getEmailContent() != null && !n.getEmailContent().isBlank()
                            && n.getRecipientEmail() != null && !n.getRecipientEmail().contains("_deleted_")) {
                        sendEmailAsync(n.getNotificationId(), n.getRecipientEmail(), n.getTitle(), n.getEmailContent());
                    }
                    processedCount++;
                } catch (Exception ex) {
                    n.setStatus(NotificationStatus.FAILED);
                    n.setErrorMessage(ex.getMessage());
                    notificationRepository.save(n);
                    log.error("[JOB-04] Lỗi gửi thông báo #{}: {}", n.getNotificationId(), ex.getMessage());
                }
            }
            log.info("[JOB-04] Đã phát hành thành công {} thông báo hẹn giờ.", processedCount);
        } catch (Exception e) {
            log.error("[JOB-04] Lỗi khi chạy tiến trình thông báo hẹn giờ: {}", e.getMessage(), e);
        }
        return processedCount;
    }

    // =========================================================================
    // CUSTOMER SCREEN #24 (UC-46: RECEIVE NOTIFICATIONS)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<Notification> getCustomerNotifications(String username, NotificationType type, Boolean unreadOnly) {
        List<Notification> list = (type != null)
                ? notificationRepository.findByRecipient_UsernameAndTypeAndStatusOrderByCreatedAtDesc(
                username, type, NotificationStatus.SENT)
                : notificationRepository.findByRecipient_UsernameAndStatusOrderByCreatedAtDesc(
                username, NotificationStatus.SENT);

        if (Boolean.TRUE.equals(unreadOnly)) {
            return list.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).collect(Collectors.toList());
        }
        return list;
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String username) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient() != null && n.getRecipient().getUsername().equals(username)) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(String username) {
        List<Notification> unreadList = notificationRepository
                .findByRecipient_UsernameAndStatusOrderByCreatedAtDesc(username, NotificationStatus.SENT);
        for (Notification n : unreadList) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(unreadList);
    }

    // =========================================================================
    // ADMIN FE-10.5: QUẢN TRỊ LỊCH SỬ, TEMPLATE & CẤU HÌNH EMAIL SMTP
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotificationsForAdmin(NotificationType type, NotificationStatus status, String keyword) {
        List<Notification> all = notificationRepository.findAllByOrderByCreatedAtDesc();
        return all.stream()
                .filter(n -> type == null || n.getType() == type)
                .filter(n -> status == null || n.getStatus() == status)
                .filter(n -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String kw = keyword.trim().toLowerCase();
                    String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
                    String msg = n.getMessage() != null ? n.getMessage().toLowerCase() : "";
                    String email = n.getRecipientEmail() != null ? n.getRecipientEmail().toLowerCase() : "";
                    String user = (n.getRecipient() != null && n.getRecipient().getUsername() != null)
                            ? n.getRecipient().getUsername().toLowerCase() : "";
                    return title.contains(kw) || msg.contains(kw) || email.contains(kw) || user.contains(kw);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getAllTemplates() {
        return templateRepository.findAllByOrderByTypeAscTemplateIdAsc();
    }

    @Override
    @Transactional
    public void updateTemplate(Long templateId, String subject, String inAppTemplate,
                               String emailHtmlTemplate, boolean emailEnabled, boolean active) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu thông báo!"));
        template.setSubject(subject);
        template.setInAppTemplate(inAppTemplate);
        template.setEmailHtmlTemplate(emailHtmlTemplate);
        template.setEmailEnabled(emailEnabled);
        template.setActive(active);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailConfig getEmailConfig() {
        return emailConfigRepository.findFirstByOrderByConfigIdAsc()
                .orElseGet(() -> EmailConfig.builder()
                        .host("smtp.gmail.com")
                        .port(587)
                        .username(environment.getProperty("spring.mail.username", "trinhhoangduc111@gmail.com"))
                        .password(environment.getProperty("spring.mail.password", ""))
                        .senderName("CineFlow Cinema")
                        .smtpAuth(true)
                        .starttlsEnable(true)
                        .globalEmailEnabled(true)
                        .build());
    }

    @Override
    @Transactional
    public void updateEmailConfig(EmailConfig updated) {
        EmailConfig config = getEmailConfig();
        config.setHost(updated.getHost());
        config.setPort(updated.getPort());
        config.setUsername(updated.getUsername());
        if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
            config.setPassword(updated.getPassword());
        }
        config.setSenderName(updated.getSenderName());
        config.setSmtpAuth(Boolean.TRUE.equals(updated.getSmtpAuth()));
        config.setStarttlsEnable(Boolean.TRUE.equals(updated.getStarttlsEnable()));
        config.setGlobalEmailEnabled(Boolean.TRUE.equals(updated.getGlobalEmailEnabled()));
        config.setUpdatedAt(LocalDateTime.now());
        emailConfigRepository.save(config);
    }

    @Override
    public void sendTestEmail(String toEmail) {
        EmailConfig cfg = getEmailConfig();
        JavaMailSenderImpl dynamicSender = buildDynamicMailSender(cfg);
        try {
            MimeMessage mimeMessage = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(cfg.getUsername(), cfg.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject("[CineFlow] Kiểm tra cấu hình hệ thống SMTP thành công");
            String html = "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #ddd;border-radius:8px;'>"
                    + "<h2 style='color:#e50914;'>✅ Cấu hình Gmail SMTP hoạt động tốt!</h2>"
                    + "<p>Email kiểm tra được gửi thành công từ hệ thống Quản trị Thông báo <strong>CineFlow Cinema</strong>.</p>"
                    + "<p><strong>Máy chủ SMTP:</strong> " + cfg.getHost() + ":" + cfg.getPort() + "</p>"
                    + "<p><strong>Tài khoản gửi:</strong> " + cfg.getUsername() + "</p>"
                    + "<p><strong>Thời gian:</strong> " + LocalDateTime.now().format(DATE_TIME_FMT) + "</p>"
                    + "</div>";
            helper.setText(html, true);
            dynamicSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Gửi email kiểm tra thất bại: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // HÀM NỘI BỘ: RENDER TEMPLATE & GỬI MAIL BẤT ĐỒNG BỘ (@Async)
    // =========================================================================
    private void createAndDispatchFromTemplate(Account recipient, String templateCode,
                                               NotificationType fallbackType, Long referenceId,
                                               Map<String, String> vars, LocalDateTime scheduledAt) {
        if (recipient == null) return;

        Optional<NotificationTemplate> optTpl = templateRepository.findByTemplateCode(templateCode);
        if (optTpl.isPresent() && !Boolean.TRUE.equals(optTpl.get().getActive())) {
            return; // Mẫu này đã bị Admin tắt
        }

        String subject = optTpl.map(NotificationTemplate::getSubject).orElse("[CineFlow] Thông báo hệ thống");
        String inAppText = optTpl.map(NotificationTemplate::getInAppTemplate).orElse("Bạn có thông báo mới từ CineFlow.");
        String emailHtml = optTpl.map(NotificationTemplate::getEmailHtmlTemplate).orElse("<p>{{message}}</p>");
        boolean emailEnabledForTemplate = optTpl.map(t -> Boolean.TRUE.equals(t.getEmailEnabled())).orElse(true);

        String finalSubject = replacePlaceholders(subject, vars);
        String finalInApp = replacePlaceholders(inAppText, vars);
        vars.put("message", finalInApp);
        String finalHtml = replacePlaceholders(emailHtml, vars);

        boolean isScheduled = scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());

        Notification notification = Notification.builder()
                .recipient(recipient)
                .recipientEmail(recipient.getEmail())
                .title(finalSubject)
                .message(finalInApp)
                .emailContent(emailEnabledForTemplate ? finalHtml : null)
                .type(optTpl.map(NotificationTemplate::getType).orElse(fallbackType))
                .templateCode(templateCode)
                .referenceId(referenceId)
                .status(isScheduled ? NotificationStatus.SCHEDULED : NotificationStatus.SENT)
                .isRead(false)
                .emailSent(false)
                .scheduledAt(scheduledAt)
                .sentAt(isScheduled ? null : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        if (!isScheduled && emailEnabledForTemplate
                && recipient.getEmail() != null
                && !recipient.getEmail().contains("_deleted_")) {
            sendEmailAsync(saved.getNotificationId(), recipient.getEmail(), finalSubject, finalHtml);
        }
    }

    @Async
    public void sendEmailAsync(Long notificationId, String toEmail, String subject, String htmlContent) {
        CompletableFuture.runAsync(() -> {
            try {
                EmailConfig cfg = getEmailConfig();
                if (!Boolean.TRUE.equals(cfg.getGlobalEmailEnabled())) {
                    log.info("Chế độ gửi Email toàn hệ thống đang TẮT. Bỏ qua gửi mail tới: {}", toEmail);
                    return;
                }

                JavaMailSenderImpl dynamicSender = buildDynamicMailSender(cfg);
                MimeMessage mimeMessage = dynamicSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(cfg.getUsername(), cfg.getSenderName());
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                dynamicSender.send(mimeMessage);

                notificationRepository.findById(notificationId).ifPresent(n -> {
                    n.setEmailSent(true);
                    n.setErrorMessage(null);
                    notificationRepository.save(n);
                });
                log.info("Đã gửi email thông báo #{} thành công tới: {}", notificationId, toEmail);
            } catch (Exception e) {
                log.error("Lỗi gửi email thông báo #{} tới {}: {}", notificationId, toEmail, e.getMessage());
                notificationRepository.findById(notificationId).ifPresent(n -> {
                    n.setErrorMessage("Lỗi SMTP: " + e.getMessage());
                    notificationRepository.save(n);
                });
            }
        });
    }

    private JavaMailSenderImpl buildDynamicMailSender(EmailConfig cfg) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(cfg.getHost());
        mailSender.setPort(cfg.getPort());
        mailSender.setUsername(cfg.getUsername());
        mailSender.setPassword(cfg.getPassword());
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(cfg.getSmtpAuth())));
        props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(cfg.getStarttlsEnable())));
        props.put("mail.smtp.starttls.required", String.valueOf(Boolean.TRUE.equals(cfg.getStarttlsEnable())));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return mailSender;
    }

    private Map<String, String> buildBookingVariables(Booking booking) {
        Map<String, String> vars = new HashMap<>();
        vars.put("bookingId", String.valueOf(booking.getBookingId()));
        vars.put("customerName", getDisplayName(booking.getAccount()));
        vars.put("totalAmount", formatCurrency(booking.getTotalAmount()));
        vars.put("status", booking.getStatus() != null ? booking.getStatus().name() : "");

        String movieTitle = "Đơn Bắp Nước (Tại quầy)";
        String branchName = "CineFlow Cinema";
        String hallName = "---";
        String showtimeStr = booking.getBookingTime() != null ? booking.getBookingTime().format(DATE_TIME_FMT) : "---";
        List<String> seats = new ArrayList<>();

        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            Ticket first = booking.getTickets().get(0);
            if (first.getShowtime() != null) {
                Showtime st = first.getShowtime();
                if (st.getMovie() != null) movieTitle = st.getMovie().getTitle();
                if (st.getHall() != null) {
                    hallName = st.getHall().getHallName();
                    if (st.getHall().getBranch() != null) {
                        branchName = st.getHall().getBranch().getBranchName();
                    }
                }
                if (st.getStartTime() != null) {
                    showtimeStr = st.getStartTime().format(DATE_TIME_FMT);
                }
            }
            for (Ticket t : booking.getTickets()) {
                if (t.getSeat() != null) {
                    seats.add(t.getSeat().getRowCode() + t.getSeat().getNumber());
                }
            }
        }

        vars.put("movieTitle", movieTitle);
        vars.put("branchName", branchName);
        vars.put("hallName", hallName);
        vars.put("showtime", showtimeStr);
        vars.put("seats", seats.isEmpty() ? "Không kèm ghế" : String.join(", ", seats));
        return vars;
    }

    private String replacePlaceholders(String template, Map<String, String> vars) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            String val = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(key, val);
        }
        return result;
    }

    private String getDisplayName(Account account) {
        if (account == null) return "Quý khách";
        if (account.getCustomer() != null && account.getCustomer().getFullName() != null) {
            return account.getCustomer().getFullName();
        }
        if (account.getStaff() != null && account.getStaff().getFullName() != null) {
            return account.getStaff().getFullName();
        }
        return account.getUsername();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        NumberFormat vn = NumberFormat.getInstance(new Locale("vi", "VN"));
        return vn.format(amount) + " đ";
    }
}