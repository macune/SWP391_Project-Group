package fu.cinema.service;

import fu.cinema.config.EmailConfig;
import fu.cinema.dto.response.PromotionResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Booking;
import fu.cinema.entity.Notification;
import fu.cinema.entity.Promotion;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.NotificationStatus;
import fu.cinema.enums.NotificationType;
import fu.cinema.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {

    //Booking Notifications
    void sendBookingConfirmedAlert(Booking booking);
    void sendBookingCancelledAlert(Booking booking);
    void sendBookingStatusUpdatedAlert(Booking booking);

    //Payment Notifications
    void sendPaymentSuccessAlert(Booking booking, PaymentMethod method, String transactionNo);
    void sendPaymentFailedAlert(Long bookingId, String reason);

    //Showtime Notifications
    void sendShowtimeChangedAlert(Showtime showtime, String oldScheduleInfo, List<Booking> affectedBookings);
    void sendShowtimeCancelledAlert(Showtime showtime, List<Booking> affectedBookings);
    int processShowtimeRemindersJob(); // JOB-03

    //Promotion Notifications & Scheduled Job
    void handleNewPromotionNotification(PromotionResponse promotion);
    int broadcastPromotionAlert(Long promotionId, LocalDateTime scheduledAt);
    int createCustomNotification(String recipientUsername, NotificationType type, String title, String message,
                                 boolean sendEmail, LocalDateTime scheduledAt);
    int processScheduledNotificationsJob(); // JOB-04

    //Customer
    List<Notification> getCustomerNotifications(String username, NotificationType type, Boolean unreadOnly);
    long countUnreadNotifications(String username);
    void markAsRead(Long notificationId, String username);
    void markAllAsRead(String username);

    //Email & Notification Management
    List<Notification> getAllNotificationsForAdmin(NotificationType type, NotificationStatus status, String keyword);
    long countByStatus(NotificationStatus status);
    List<Promotion> getAllPromotions();
    List<Account> getActiveAccounts();
    List<EmailConfig.TemplateItem> getAllTemplates();
    void updateTemplate(Long templateId, String subject, String inAppTemplate, String emailHtmlTemplate,
                        boolean emailEnabled, boolean active);
    EmailConfig getEmailConfig();
    void updateEmailConfig(EmailConfig updatedConfig);
    void sendTestEmail(String toEmail);
}