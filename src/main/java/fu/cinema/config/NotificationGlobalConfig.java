package fu.cinema.config;

import fu.cinema.enums.NotificationStatus;
import fu.cinema.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Configuration
@EnableAsync
@EnableScheduling
@ControllerAdvice
@RequiredArgsConstructor
public class NotificationGlobalConfig {

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