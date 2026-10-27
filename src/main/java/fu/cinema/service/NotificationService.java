package fu.cinema.service;

import fu.cinema.entity.*;
import fu.cinema.enums.NotificationStatus;
import fu.cinema.enums.NotificationType;
import fu.cinema.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {

    // FE-10.1: Booking Notifications
    void sendBookingConfirmedAlert(Booking booking);
    void sendBookingCancelledAlert(Booking booking);
    void sendBookingStatusUpdatedAlert(Booking booking);

    // FE-10.2: Payment Notifications
    void sendPaymentSuccessAlert(Booking booking, PaymentMethod method, String transactionNo);
    void sendPaymentFailedAlert(Long bookingId, String reason);

    // FE-10.3: Showtime Notifications
    void sendShowtimeChangedAlert(Showtime showtime, String oldScheduleInfo, List<Booking> affectedBookings);
    void sendShowtimeCancelledAlert(Showtime showtime, List<Booking> affectedBookings);
    int processShowtimeRemindersJob(); // JOB-03

    // FE-10.4: Promotion Notifications & Scheduled Job
    void handleNewPromotionNotification(Promotion promotion);
    int broadcastPromotionAlert(Long promotionId, LocalDateTime scheduledAt);
    int createCustomNotification(String recipientUsername, NotificationType type, String title, String message,
                                 boolean sendEmail, LocalDateTime scheduledAt);
    int processScheduledNotificationsJob(); // JOB-04

    // Customer Screen #24 (UC-46: Receive Notifications)
    List<Notification> getCustomerNotifications(String username, NotificationType type, Boolean unreadOnly);
    void markAsRead(Long notificationId, String username);
    void markAllAsRead(String username);

    // Admin FE-10.5: Email & Notification Management (UC-43)
    List<Notification> getAllNotificationsForAdmin(NotificationType type, NotificationStatus status, String keyword);
    List<NotificationTemplate> getAllTemplates();
    void updateTemplate(Long templateId, String subject, String inAppTemplate, String emailHtmlTemplate,
                        boolean emailEnabled, boolean active);
    EmailConfig getEmailConfig();
    void updateEmailConfig(EmailConfig updatedConfig);
    void sendTestEmail(String toEmail);
}