package fu.cinema.service.impl;

import fu.cinema.dto.response.AccountResponse;
import fu.cinema.entity.Account;
import fu.cinema.enums.AccountStatus;
import fu.cinema.exception.AccountNotFoundException;
import fu.cinema.exception.AppException;
import fu.cinema.mapper.AccountMapper;
import fu.cinema.repository.AccountRepository;
import fu.cinema.service.AccountService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountServiceImpl implements AccountService {
    AccountRepository accountRepository;
    AccountMapper accountMapper;

    @Override
    public List<AccountResponse> getAllAccounts() {
        List<Account> list = accountRepository.findByStatusIn(
                List.of(AccountStatus.ACTIVE, AccountStatus.LOCKED, AccountStatus.DELETED)
        );
        return list.stream().map(accountMapper::toAccountResponse).toList();
    }

    @Override
    @Transactional
    public void changeAccountStatus(Long accountId, AccountStatus status) {
        Account account = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && account.getUsername().equals(auth.getName())) {
            throw new AppException("Bạn không thể thay đổi trạng thái của chính mình.");
        }

        if (account.getStatus() == AccountStatus.DELETED) {
            throw new AppException("Tài khoản đã bị xóa không thể thay đổi trạng thái.");
        }

        if (status == AccountStatus.DELETED) {
            if (!account.getEmail().contains("_deleted_")) {
                account.setEmail(account.getEmail() + "_deleted_" + System.currentTimeMillis());
            }
            if (account.getStaff() != null) {
                account.getStaff().setBranch(null);
            }
        }

        account.setStatus(status);
        accountRepository.save(account);
    }

    @Override
    public List<AccountResponse> searchAccounts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAccounts();
        }
        List<Account> list = accountRepository.findByUsernameContainingIgnoreCaseAndStatusIn(
                keyword.trim(),
                List.of(AccountStatus.ACTIVE, AccountStatus.LOCKED, AccountStatus.DELETED)
        );
        return list.stream().map(accountMapper::toAccountResponse).toList();
    }
}