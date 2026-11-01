package fu.cinema.service.impl;

import fu.cinema.dto.request.*;
import fu.cinema.dto.response.CustomerResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Customer;
import fu.cinema.entity.VerificationToken;
import fu.cinema.enums.AccountStatus;
import fu.cinema.exception.*;
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
import java.util.List;
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
                .expiryDate(LocalDateTime.now().plusMinutes(10))
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

    //Xử lý yêu cầu quên mật khẩu: tạo token đặt lại mật khẩu và gửi email hướng dẫn
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra tài khoản có tồn tại và đang ở trạng thái ACTIVE hay không
        Account account = accountRepository.findByEmailAndStatus(email, AccountStatus.ACTIVE)
                .orElseThrow(() -> new AppException("Tài khoản không tồn tại hoặc chưa được kích hoạt."));

        String forgotPasswordToken = UUID.randomUUID().toString();

        // 2. Tìm token hiện có của tài khoản hoặc khởi tạo mới (tránh trùng khóa UNIQUE account_id)
        VerificationToken verificationToken = account.getVerificationToken();
        if (verificationToken == null) {
            verificationToken = new VerificationToken();
            verificationToken.setAccount(account);
            account.setVerificationToken(verificationToken);
        }

        // 3. Cập nhật mã token mới và thời hạn hiệu lực (10 phút)
        verificationToken.setToken(forgotPasswordToken);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        verificationTokenRepository.save(verificationToken);

        // 4. Gửi email chứa đường dẫn đặt lại mật khẩu
        emailService.sendResetPasswordEmail(email, forgotPasswordToken);
    }

    //Xác thực token đặt lại mật khẩu khi người dùng bấm vào đường link trong email
    @Override
    @Transactional
    public void validateResetPasswordToken(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(EmailTokenNotFoundException::new);

        // Kiểm tra nếu token đã hết hạn thì xóa khỏi database và ném ngoại lệ
        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new EmailTokenExpiredException();
        }
    }

    //Cập nhật mật khẩu mới cho người dùng sau khi xác thực token thành công
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        // 1. Kiểm tra khớp mật khẩu xác nhận
        if (!resetPasswordRequest.getPassword().equals(resetPasswordRequest.getConfirmPassword())) {
            throw new PasswordNotMatchException();
        }

        // 2. Tìm và kiểm tra thời hạn token một lần nữa để đảm bảo tính an toàn
        VerificationToken verificationToken = verificationTokenRepository.findByToken(resetPasswordRequest.getToken())
                .orElseThrow(EmailTokenNotFoundException::new);

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new EmailTokenExpiredException();
        }

        // 3. Mã hóa mật khẩu mới và cập nhật cho tài khoản
        Account account = verificationToken.getAccount();
        String passwordHash = passwordEncoder.encode(resetPasswordRequest.getPassword());
        account.setPasswordHash(passwordHash);

        // 4. Hủy token sau khi đã đổi mật khẩu thành công (chỉ sử dụng 1 lần)
        account.setVerificationToken(null);
        accountRepository.save(account);
        verificationTokenRepository.delete(verificationToken);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerRequest getCustomerProfile(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản"));

        CustomerRequest request = new CustomerRequest();
        if (account.getCustomer() != null) {
            request.setFullName(account.getCustomer().getFullName());
            request.setPhone(account.getCustomer().getPhone());
        }
        return request;
    }

    @Override
    @Transactional
    public void updateCustomerProfile(String username, CustomerRequest request) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản"));

        Customer customer = account.getCustomer();
        if (customer == null) {
            customer = new Customer();
            customer.setAccount(account);
            account.setCustomer(customer);
        }

        customer.setFullName(request.getFullName().trim());
        customer.setPhone(request.getPhone().trim());
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp!");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw new AppException("Mật khẩu hiện tại không chính xác!");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }
}
