package fu.cinema.controller.admin;

import fu.cinema.entity.EmailConfig;
import fu.cinema.entity.Notification;
import fu.cinema.entity.NotificationTemplate;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.NotificationStatus;
import fu.cinema.enums.NotificationType;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.NotificationRepository;
import fu.cinema.repository.PromotionRepository;
import fu.cinema.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final PromotionRepository promotionRepository;
    private final AccountRepository accountRepository;

    // Giao diện All-in-One 3 Tabs (Phương án 4A)
    @GetMapping("/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewNotificationManagement(
            @RequestParam(value = "tab", defaultValue = "history") String activeTab,
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "status", required = false) NotificationStatus status,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        List<Notification> history = notificationService.getAllNotificationsForAdmin(type, status, keyword);
        List<NotificationTemplate> templates = notificationService.getAllTemplates();
        EmailConfig emailConfig = notificationService.getEmailConfig();

        model.addAttribute("activeTab", activeTab);
        model.addAttribute("notifications", history);
        model.addAttribute("templates", templates);
        model.addAttribute("emailConfig", emailConfig);
        model.addAttribute("promotions", promotionRepository.findAll());
        model.addAttribute("accounts", accountRepository.findByStatusIn(List.of(AccountStatus.ACTIVE)));

        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("types", NotificationType.values());
        model.addAttribute("statuses", NotificationStatus.values());

        // Thống kê nhanh trên đầu trang
        model.addAttribute("totalSent", notificationRepository.countByStatus(NotificationStatus.SENT));
        model.addAttribute("totalScheduled", notificationRepository.countByStatus(NotificationStatus.SCHEDULED));
        model.addAttribute("totalFailed", notificationRepository.countByStatus(NotificationStatus.FAILED));

        return "admin/notification-management";
    }

    // Gửi thông báo tùy chỉnh hoặc hẹn giờ gửi (JOB-04)
    @PostMapping("/admin/notifications/send-custom")
    @PreAuthorize("hasRole('ADMIN')")
    public String sendCustomNotification(
            @RequestParam("recipientUsername") String recipientUsername,
            @RequestParam("type") NotificationType type,
            @RequestParam("title") String title,
            @RequestParam("message") String message,
            @RequestParam(value = "sendEmail", defaultValue = "false") boolean sendEmail,
            @RequestParam(value = "scheduledAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
            RedirectAttributes redirectAttributes) {
        try {
            int count = notificationService.createCustomNotification(
                    recipientUsername, type, title, message, sendEmail, scheduledAt);
            if (scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã lên lịch hẹn giờ gửi " + count + " thông báo (JOB-04 sẽ tự động phát hành khi đến hạn)!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã phát hành thành công " + count + " thông báo!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi thông báo: " + e.getMessage());
        }
        return "redirect:/admin/notifications?tab=history";
    }

    // Phát thông báo Khuyến mãi / Mã giảm giá hàng loạt (FE-10.4)
    @PostMapping("/admin/notifications/broadcast-promotion")
    @PreAuthorize("hasRole('ADMIN')")
    public String broadcastPromotion(
            @RequestParam("promotionId") Long promotionId,
            @RequestParam(value = "scheduledAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
            RedirectAttributes redirectAttributes) {
        try {
            int count = notificationService.broadcastPromotionAlert(promotionId, scheduledAt);
            if (scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã hẹn giờ gửi thông báo khuyến mãi cho " + count + " khách hàng!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã gửi thông báo khuyến mãi thành công cho " + count + " khách hàng!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phát thông báo khuyến mãi: " + e.getMessage());
        }
        return "redirect:/admin/notifications?tab=history";
    }

    // Nút kích hoạt chạy ngay JOB-03 & JOB-04 phục vụ Demo
    @PostMapping("/admin/notifications/run-jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public String triggerBackgroundJobsNow(RedirectAttributes redirectAttributes) {
        try {
            int reminders = notificationService.processShowtimeRemindersJob();
            int scheduled = notificationService.processScheduledNotificationsJob();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chạy quét ngầm thành công! [JOB-03 Nhắc lịch chiếu: " + reminders + " thông báo] - [JOB-04 Phát hành hẹn giờ: " + scheduled + " thông báo].");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chạy Background Jobs: " + e.getMessage());
        }
        return "redirect:/admin/notifications?tab=history";
    }

    // Cập nhật Mẫu Template Email/In-App (Tab 2 - FE-10.5)
    @PostMapping("/admin/notifications/templates/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateTemplate(
            @RequestParam("templateId") Long templateId,
            @RequestParam("subject") String subject,
            @RequestParam("inAppTemplate") String inAppTemplate,
            @RequestParam("emailHtmlTemplate") String emailHtmlTemplate,
            @RequestParam(value = "emailEnabled", defaultValue = "false") boolean emailEnabled,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.updateTemplate(templateId, subject, inAppTemplate, emailHtmlTemplate, emailEnabled, active);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật mẫu thông báo/email thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật mẫu: " + e.getMessage());
        }
        return "redirect:/admin/notifications?tab=templates";
    }

    // Cập nhật Cấu hình máy chủ Gmail SMTP (Tab 3 - FE-10.5)
    @PostMapping("/admin/notifications/email-config/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateEmailConfig(@ModelAttribute EmailConfig emailConfig, RedirectAttributes redirectAttributes) {
        try {
            notificationService.updateEmailConfig(emailConfig);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu cấu hình máy chủ Gmail SMTP thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu cấu hình SMTP: " + e.getMessage());
        }
        return "redirect:/admin/notifications?tab=config";
    }

    // Gửi Email Kiểm Tra (Test Mail) tại Tab 3
    @PostMapping("/admin/notifications/email-config/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String sendTestEmail(@RequestParam("testEmail") String testEmail, RedirectAttributes redirectAttributes) {
        try {
            notificationService.sendTestEmail(testEmail);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi Email kiểm tra thành công tới địa chỉ: " + testEmail);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/notifications?tab=config";
    }

    // API-05 chuẩn đặc tả SRS (Mục 5.5: POST /api/v1/notifications/send)
    @PostMapping("/api/v1/notifications/send")
    @ResponseBody
    public ResponseEntity<?> sendNotificationApi(@RequestBody Map<String, Object> payload) {
        try {
            String recipient = (String) payload.getOrDefault("recipientUsername", "ALL");
            String typeStr = (String) payload.getOrDefault("type", "PROMOTION");
            String title = (String) payload.getOrDefault("title", "Thông báo từ CineFlow");
            String message = (String) payload.getOrDefault("message", "");
            boolean sendEmail = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("sendEmail", "true")));

            int sentCount = notificationService.createCustomNotification(
                    recipient, NotificationType.valueOf(typeStr), title, message, sendEmail, null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sentCount", sentCount,
                    "message", "Đã gửi thông báo thành công qua API-05"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}