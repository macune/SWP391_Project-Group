package fu.cinema.service;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.Customer;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import fu.cinema.exception.CustomerAlreadyExistsException;
import fu.cinema.mapper.CustomerMapper;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.CustomerRepository;
import fu.cinema.repository.VerificationTokenRepository;
import fu.cinema.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerCreationRequest request;

    @BeforeEach
    void setUp() {
        request = CustomerCreationRequest.builder()
                .username("nguyenvana")
                .password("123456")
                .confirmPassword("123456")
                .fullName("Nguyễn Văn A")
                .email("nguyenvana@gmail.com")
                .phone("0912345678")
                .build();
    }

    @Test
    void testRegisterSuccess() {
        when(accountRepository.findByUsername("nguyenvana")).thenReturn(java.util.Optional.empty());
        when(accountRepository.findByEmail("nguyenvana@gmail.com")).thenReturn(java.util.Optional.empty());
        when(customerRepository.findByPhone("0912345678")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Account account = Account.builder()
                .role(Role.CUSTOMER)
                .status(AccountStatus.INACTIVE)
                .build();
        Customer customer = Customer.builder()
                .emailVerified(false)
                .build();

        when(customerMapper.toAccount(request)).thenReturn(account);
        when(customerMapper.toCustomer(request)).thenReturn(customer);
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = customerService.register(request);

        assertNotNull(created);
        assertEquals("Nguyễn Văn A", created.getFullName());
        assertEquals("0912345678", created.getPhone());
        assertNotNull(created.getAccount());
        assertEquals("nguyenvana", created.getAccount().getUsername());
        assertEquals("hashed_password", created.getAccount().getPasswordHash());
        assertEquals(Role.CUSTOMER, created.getAccount().getRole()); // Luôn là CUSTOMER
        assertEquals(AccountStatus.INACTIVE, created.getAccount().getStatus()); // Chờ xác thực email
        assertFalse(created.getEmailVerified());

        verify(accountRepository, times(1)).save(any());
        verify(verificationTokenRepository, times(1)).save(any());
        verify(emailService, times(1)).sendVerificationEmail(eq("nguyenvana@gmail.com"), anyString());
    }

    @Test
    void testRegisterThrowsWhenUsernameExists() {
        when(accountRepository.findByUsername("nguyenvana"))
                .thenReturn(java.util.Optional.of(Account.builder().status(AccountStatus.ACTIVE).build()));

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.register(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testRegisterThrowsWhenEmailExists() {
        when(accountRepository.findByUsername("nguyenvana")).thenReturn(java.util.Optional.empty());
        when(accountRepository.findByEmail("nguyenvana@gmail.com"))
                .thenReturn(java.util.Optional.of(Account.builder().status(AccountStatus.ACTIVE).build()));

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.register(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testRegisterThrowsWhenPhoneExists() {
        when(accountRepository.findByUsername("nguyenvana")).thenReturn(java.util.Optional.empty());
        when(accountRepository.findByEmail("nguyenvana@gmail.com")).thenReturn(java.util.Optional.empty());
        when(customerRepository.findByPhone("0912345678"))
                .thenReturn(java.util.Optional.of(Customer.builder().account(Account.builder().status(AccountStatus.ACTIVE).build()).build()));

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.register(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testRegisterCleansUpInactiveAccountWhenReRegistering() {
        Account inactiveAccount = Account.builder()
                .username("nguyenvana")
                .status(AccountStatus.INACTIVE)
                .build();
        when(accountRepository.findByUsername("nguyenvana")).thenReturn(java.util.Optional.of(inactiveAccount));
        when(accountRepository.findByEmail("nguyenvana@gmail.com")).thenReturn(java.util.Optional.empty());
        when(customerRepository.findByPhone("0912345678")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Account account = Account.builder()
                .role(Role.CUSTOMER)
                .status(AccountStatus.INACTIVE)
                .build();
        Customer customer = Customer.builder()
                .emailVerified(false)
                .build();

        when(customerMapper.toAccount(request)).thenReturn(account);
        when(customerMapper.toCustomer(request)).thenReturn(customer);
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = customerService.register(request);

        assertNotNull(created);
        verify(verificationTokenRepository, times(1)).deleteByAccount(inactiveAccount);
        verify(accountRepository, times(1)).delete(inactiveAccount);
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    void testVerifyEmailSuccess() {
        Account account = Account.builder()
                .status(AccountStatus.INACTIVE)
                .build();
        Customer customer = Customer.builder()
                .emailVerified(false)
                .build();
        account.setCustomer(customer);

        fu.cinema.entity.VerificationToken token = fu.cinema.entity.VerificationToken.builder()
                .token("valid-token")
                .account(account)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(10))
                .build();

        when(verificationTokenRepository.findByToken("valid-token")).thenReturn(java.util.Optional.of(token));

        customerService.verifyEmail("valid-token");

        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertTrue(customer.getEmailVerified());
        verify(accountRepository, times(1)).save(account);
        verify(verificationTokenRepository, times(1)).delete(token);
    }

    @Test
    void testVerifyEmailTokenNotFound() {
        when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(java.util.Optional.empty());

        assertThrows(fu.cinema.exception.EmailTokenNotFoundException.class, () -> customerService.verifyEmail("invalid-token"));
    }

    @Test
    void testVerifyEmailTokenExpired() {
        Account account = Account.builder().status(AccountStatus.INACTIVE).build();
        fu.cinema.entity.VerificationToken token = fu.cinema.entity.VerificationToken.builder()
                .token("expired-token")
                .account(account)
                .expiryDate(java.time.LocalDateTime.now().minusMinutes(5))
                .build();

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(java.util.Optional.of(token));

        assertThrows(fu.cinema.exception.EmailTokenExpiredException.class, () -> customerService.verifyEmail("expired-token"));
        verify(verificationTokenRepository, times(1)).delete(token);
        verify(accountRepository, times(1)).delete(account);
    }
}
