package fu.cinema.service;

import fu.cinema.dto.response.AccountResponse;
import fu.cinema.enums.AccountStatus;

import java.util.List;

public interface AccountService {
    List<AccountResponse> getAllAccounts();
    void changeAccountStatus(Long accountId, AccountStatus status);
    List<AccountResponse> searchAccounts(String keyword);
}
