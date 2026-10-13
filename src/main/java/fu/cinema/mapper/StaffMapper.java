package fu.cinema.mapper;

import fu.cinema.dto.request.StaffRequest;
import fu.cinema.dto.response.StaffResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffMapper {
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "staff", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    Account toAccount(StaffRequest request);

    @Mapping(target = "staffId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "branch", ignore = true)
    Staff toStaff(StaffRequest request);

    @Mapping(target = "staffId", source = "staffId")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "branchName", source = "branch.branchName")
    @Mapping(target = "branchId", source = "branch.branchId")
    StaffResponse toStaffResponse(Staff staff);
}
