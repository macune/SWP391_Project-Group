package fu.cinema.controller.customer;

import fu.cinema.entity.Notification;
import fu.cinema.enums.NotificationType;
import fu.cinema.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/notifications")
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String viewMyNotifications(
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();
        List<Notification> notifications = notificationService.getCustomerNotifications(username, type, unreadOnly);

        model.addAttribute("notifications", notifications);
        model.addAttribute("selectedType", type);
        model.addAttribute("unreadOnly", Boolean.TRUE.equals(unreadOnly));
        model.addAttribute("types", NotificationType.values());

        return "customer/notifications";
    }

    @PostMapping("/read/{id}")
    public String markOneAsRead(@PathVariable("id") Long id, Authentication authentication) {
        notificationService.markAsRead(id, authentication.getName());
        return "redirect:/customer/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        notificationService.markAllAsRead(authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu tất cả thông báo là đã đọc!");
        return "redirect:/customer/notifications";
    }
}