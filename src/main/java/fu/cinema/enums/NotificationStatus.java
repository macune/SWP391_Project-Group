package fu.cinema.enums;

public enum NotificationStatus {
    SCHEDULED,  // Đã lên lịch hẹn giờ gửi (Chờ JOB-04 quét)
    SENT,       // Đã gửi thành công
    FAILED      // Gửi thất bại (Sai email hoặc lỗi dịch vụ)
}