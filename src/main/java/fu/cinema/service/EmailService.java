package fu.cinema.service;

import fu.cinema.entity.Account;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
}
