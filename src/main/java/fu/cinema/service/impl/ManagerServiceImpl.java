package fu.cinema.service.impl;

import fu.cinema.dto.response.StaffResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Staff;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import fu.cinema.mapper.StaffMapper;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.ManagerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerServiceImpl implements ManagerService {
    StaffRepository staffRepository;
    AccountRepository accountRepository;
    StaffMapper staffMapper;

    @Override
    public Branch getCurrentManagerBranch() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String username = auth.getName();
        return accountRepository.findByUsername(username)
                .map(Account::getStaff)
                .map(Staff::getBranch)
                .orElse(null);
    }

    @Override
    public List<StaffResponse> getAllStaffs() {
        Branch branch = getCurrentManagerBranch();
        if (branch == null) {
            return Collections.emptyList();
        }
        List<Staff> list = staffRepository.findByBranch_BranchIdAndAccount_RoleAndAccount_StatusNot(branch.getBranchId(), Role.STAFF, AccountStatus.DELETED);
        return list.stream().map(staffMapper::toStaffResponse).toList();
    }
}
