package fu.cinema.service.impl;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.Customer;
import fu.cinema.exception.CustomerAlreadyExistsException;
import fu.cinema.exception.ErrorCode;
import fu.cinema.mapper.CustomerMapper;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.CustomerRepository;
import fu.cinema.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public Customer register(CustomerCreationRequest request) {

        // 1. Kiểm tra xem Username + Email + Phone đã tồn tại chưa
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new CustomerAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new CustomerAlreadyExistsException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Map request thành Account
        Account account = customerMapper.toAccount(request);
        account.setUsername(request.getUsername().trim());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setEmail(request.getEmail().trim().toLowerCase());

        // 3. Map request thành Customer
        Customer customer = customerMapper.toCustomer(request);
        customer.setFullName(request.getFullName().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setAccount(account);

        account.setCustomer(customer);

        // Lưu account (sẽ cascade lưu customer)
        accountRepository.save(account);
        return customer;
    }
}
