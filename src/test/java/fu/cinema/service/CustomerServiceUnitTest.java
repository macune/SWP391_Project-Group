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
        when(accountRepository.existsByUsername("nguyenvana")).thenReturn(false);
        when(accountRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(false);
        when(customerRepository.existsByPhone("0912345678")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        Account account = Account.builder()
                .role(Role.CUSTOMER)
                .status(AccountStatus.ACTIVE)
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
        assertFalse(created.getEmailVerified());

        verify(accountRepository, times(1)).save(any());
        verify(customerMapper, times(1)).toAccount(request);
        verify(customerMapper, times(1)).toCustomer(request);
    }

    @Test
    void testRegisterThrowsWhenUsernameExists() {
        when(accountRepository.existsByUsername("nguyenvana")).thenReturn(true);

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.register(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testRegisterThrowsWhenEmailExists() {
        when(accountRepository.existsByUsername("nguyenvana")).thenReturn(false);
        when(accountRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(true);

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.register(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testRegisterThrowsWhenPhoneExists() {
        when(accountRepository.existsByUsername("nguyenvana")).thenReturn(false);
        when(accountRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(false);
        when(customerRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.register(request));
        verify(accountRepository, never()).save(any());
    }
}
