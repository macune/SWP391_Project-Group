package fu.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "smtp_host", nullable = false, length = 150)
    @Builder.Default
    private String host = "smtp.gmail.com";

    @Column(name = "smtp_port", nullable = false)
    @Builder.Default
    private Integer port = 587;

    @Column(name = "smtp_username", nullable = false, length = 150)
    private String username;

    @Column(name = "smtp_password", nullable = false, length = 255)
    private String password;

    @Column(name = "sender_name", nullable = false, columnDefinition = "NVARCHAR(150)")
    @Builder.Default
    private String senderName = "CineFlow Cinema";

    @Column(name = "smtp_auth", nullable = false)
    @Builder.Default
    private Boolean smtpAuth = true;

    @Column(name = "starttls_enable", nullable = false)
    @Builder.Default
    private Boolean starttlsEnable = true;

    @Column(name = "global_email_enabled", nullable = false)
    @Builder.Default
    private Boolean globalEmailEnabled = true;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}