package fu.cinema.mapper;

import fu.cinema.dto.request.CustomerCreationRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", constant = "CUSTOMER")
    @Mapping(target = "status", constant = "INACTIVE")
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "staff", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    Account toAccount(CustomerCreationRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "emailVerified", constant = "false")
    Customer toCustomer(CustomerCreationRequest request);
}
