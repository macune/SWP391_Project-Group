package fu.cinema.entity;

import fu.cinema.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_code", nullable = false, unique = true, length = 100)
    private String templateCode;

    @Column(name = "template_name", nullable = false, columnDefinition = "NVARCHAR(200)")
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "subject", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String subject;

    @Column(name = "in_app_template", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String inAppTemplate;

    @Column(name = "email_html_template", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String emailHtmlTemplate;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}