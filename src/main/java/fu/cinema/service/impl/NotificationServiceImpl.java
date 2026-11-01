package fu.cinema.service.impl;

import fu.cinema.config.EmailConfig;
import fu.cinema.entity.*;
import fu.cinema.enums.*;
import fu.cinema.repository.*;
import fu.cinema.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final PromotionRepository promotionRepository;
    private final EmailConfig emailConfig;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

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
    public List<EmailConfig.TemplateItem> getAllTemplates() {
        return emailConfig.getTemplates();
    }

    @Override
    public void updateTemplate(Long templateId, String subject, String inAppTemplate,
                               String emailHtmlTemplate, boolean emailEnabled, boolean active) {
        EmailConfig.TemplateItem template = emailConfig.findTemplateById(templateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu thông báo!"));
        template.setSubject(subject);
        template.setInAppTemplate(inAppTemplate);
        template.setEmailHtmlTemplate(emailHtmlTemplate);
        template.setEmailEnabled(emailEnabled);
        template.setActive(active);
        template.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public EmailConfig getEmailConfig() {
        return emailConfig;
    }

    @Override
    public void updateEmailConfig(EmailConfig updated) {
        emailConfig.setHost(updated.getHost());
        emailConfig.setPort(updated.getPort());
        emailConfig.setUsername(updated.getUsername());
        if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
            emailConfig.setPassword(updated.getPassword());
        }
        emailConfig.setSenderName(updated.getSenderName());
        emailConfig.setSmtpAuth(Boolean.TRUE.equals(updated.getSmtpAuth()));
        emailConfig.setStarttlsEnable(Boolean.TRUE.equals(updated.getStarttlsEnable()));
        emailConfig.setGlobalEmailEnabled(Boolean.TRUE.equals(updated.getGlobalEmailEnabled()));
        emailConfig.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public void sendTestEmail(String toEmail) {
        JavaMailSenderImpl dynamicSender = buildDynamicMailSender(emailConfig);
        try {
            MimeMessage mimeMessage = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(emailConfig.getUsername(), emailConfig.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject("[CineFlow] Kiểm tra cấu hình hệ thống SMTP thành công");
            String html = "<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #ddd;border-radius:8px;'>"
                    + "<h2 style='color:#e50914;'>✅ Cấu hình Gmail SMTP hoạt động tốt!</h2>"
                    + "<p>Email kiểm tra được gửi thành công từ hệ thống Quản trị Thông báo <strong>CineFlow Cinema</strong>.</p>"
                    + "<p><strong>Máy chủ SMTP:</strong> " + emailConfig.getHost() + ":" + emailConfig.getPort() + "</p>"
                    + "<p><strong>Tài khoản gửi:</strong> " + emailConfig.getUsername() + "</p>"
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

        Optional<EmailConfig.TemplateItem> optTpl = emailConfig.findTemplateByCode(templateCode);
        if (optTpl.isPresent() && !Boolean.TRUE.equals(optTpl.get().getActive())) {
            return; // Mẫu này đang bị Admin tắt
        }

        String subject = optTpl.map(EmailConfig.TemplateItem::getSubject).orElse("[CineFlow] Thông báo hệ thống");
        String inAppText = optTpl.map(EmailConfig.TemplateItem::getInAppTemplate).orElse("Bạn có thông báo mới từ CineFlow.");
        String emailHtml = optTpl.map(EmailConfig.TemplateItem::getEmailHtmlTemplate).orElse("<p>{{message}}</p>");
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
                .type(optTpl.map(EmailConfig.TemplateItem::getType).orElse(fallbackType))
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
                if (!Boolean.TRUE.equals(emailConfig.getGlobalEmailEnabled())) {
                    log.info("Chế độ gửi Email toàn hệ thống đang TẮT. Bỏ qua gửi mail tới: {}", toEmail);
                    return;
                }

                JavaMailSenderImpl dynamicSender = buildDynamicMailSender(emailConfig);
                MimeMessage mimeMessage = dynamicSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(emailConfig.getUsername(), emailConfig.getSenderName());
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