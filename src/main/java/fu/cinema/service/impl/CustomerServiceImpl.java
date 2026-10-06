package fu.cinema.service.impl;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.Customer;
import fu.cinema.entity.VerificationToken;
import fu.cinema.enums.AccountStatus;
import fu.cinema.exception.CustomerAlreadyExistsException;
import fu.cinema.exception.EmailTokenExpiredException;
import fu.cinema.exception.EmailTokenNotFoundException;
import fu.cinema.exception.ErrorCode;
import fu.cinema.mapper.CustomerMapper;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.CustomerRepository;
import fu.cinema.repository.VerificationTokenRepository;
import fu.cinema.service.CustomerService;
import fu.cinema.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;

    @Override
    @Transactional
    public Customer register(CustomerCreationRequest request) {

        // 1. Kiểm tra xem Username + Email + Phone đã tồn tại chưa
        // Nếu đã tồn tại nhưng tài khoản chưa kích hoạt (INACTIVE) -> dọn dẹp bản ghi cũ để cho phép đăng ký lại
        accountRepository.findByUsername(request.getUsername().trim()).ifPresent(existingAccount -> {
            if (existingAccount.getStatus() == AccountStatus.INACTIVE) {
                verificationTokenRepository.deleteByAccount(existingAccount);
                accountRepository.delete(existingAccount);
                accountRepository.flush();
            } else {
                throw new CustomerAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
        });

        accountRepository.findByEmail(request.getEmail().trim().toLowerCase()).ifPresent(existingAccount -> {
            if (existingAccount.getStatus() == AccountStatus.INACTIVE) {
                verificationTokenRepository.deleteByAccount(existingAccount);
                accountRepository.delete(existingAccount);
                accountRepository.flush();
            } else {
                throw new CustomerAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        });

        customerRepository.findByPhone(request.getPhone().trim()).ifPresent(existingCustomer -> {
            Account existingAccount = existingCustomer.getAccount();
            if (existingAccount != null && existingAccount.getStatus() == AccountStatus.INACTIVE) {
                verificationTokenRepository.deleteByAccount(existingAccount);
                accountRepository.delete(existingAccount);
                accountRepository.flush();
            } else {
                throw new CustomerAlreadyExistsException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        });

        // 2. Map request thành Account (mặc định trạng thái INACTIVE chờ xác thực email)
        Account account = customerMapper.toAccount(request);
        account.setUsername(request.getUsername().trim());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setEmail(request.getEmail().trim().toLowerCase());
        account.setStatus(AccountStatus.INACTIVE);

        // 3. Map request thành Customer
        Customer customer = customerMapper.toCustomer(request);
        customer.setFullName(request.getFullName().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setAccount(account);

        account.setCustomer(customer);

        // Lưu account (sẽ cascade lưu customer)
        Account savedAccount = accountRepository.save(account);
        String emailToken = UUID.randomUUID().toString();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(emailToken)
                .account(savedAccount)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .build();
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedAccount.getEmail(), emailToken);
        return customer;
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(EmailTokenNotFoundException::new);

        Account account = verificationToken.getAccount();

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            if (account != null && account.getStatus() == AccountStatus.INACTIVE) {
                accountRepository.delete(account);
            }
            throw new EmailTokenExpiredException();
        }

        account.setStatus(AccountStatus.ACTIVE);
        if (account.getCustomer() != null) {
            account.getCustomer().setEmailVerified(true);
        }
        accountRepository.save(account);

        // Xóa token sau khi kích hoạt thành công
        verificationTokenRepository.delete(verificationToken);
    }
}
