package fu.cinema.service.impl;

import fu.cinema.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {
    JavaMailSender javaMailSender;
    Environment environment;

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String confirmationUrl = "http://localhost:8080/verify-email?token=" + token;

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Set sender, receiver, email title
            String fromEmail = environment.getProperty("spring.mail.username");
            if (fromEmail == null || fromEmail.isBlank()) {
                fromEmail = "trinhhoangduc111@gmail.com";
            }
            helper.setFrom(fromEmail, "CineFlow Cinema");
            helper.setTo(toEmail);
            helper.setSubject("Kích hoạt tài khoản CineFlow");

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
                    + "<h2 style='color: #e50914;'>Chào mừng bạn đến với CineFlow!</h2>"
                    + "<p>Vui lòng bấm vào nút bên dưới để xác thực tài khoản:</p>"
                    + "<p style='margin: 25px 0;'>"
                    + "   <a href='" + confirmationUrl + "' style='background-color: #e50914; color: white; padding: 12px 25px; text-decoration: none; border-radius: 4px; font-weight: bold;'>Xác Nhận Tài Khoản</a>"
                    + "</p>"
                    + "<p>Đường dẫn này có hiệu lực trong vòng <strong>30 phút</strong>.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            log.info("Đang gửi email xác thực tới: {}", toEmail);
            javaMailSender.send(mimeMessage);
            log.info("Đã gửi email xác thực thành công tới: {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email xác thực thất bại tới {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
