package fu.cinema.service.impl;

import fu.cinema.dto.response.StaffResponse;
import fu.cinema.entity.Staff;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import fu.cinema.mapper.StaffMapper;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.AdminService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminServiceImpl implements AdminService {
    StaffRepository staffRepository;
    StaffMapper staffMapper;

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> getAllManagers() {
        List<Staff> managerList = staffRepository.findByAccount_RoleAndAccount_StatusNot(Role.MANAGER, AccountStatus.DELETED);
        return managerList.stream().map(staffMapper::toStaffResponse).toList();
    }
}
