package fu.cinema.mapper;

import fu.cinema.dto.response.AccountResponse;
import fu.cinema.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponse toAccountResponse(Account account);
}
