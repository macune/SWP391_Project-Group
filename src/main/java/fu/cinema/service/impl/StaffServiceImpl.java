package fu.cinema.service.impl;

import fu.cinema.dto.request.ManagerRequest;
import fu.cinema.dto.request.StaffRequest;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Staff;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.Role;
import fu.cinema.exception.*;
import fu.cinema.mapper.StaffMapper;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.StaffService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffServiceImpl implements StaffService {
    AccountRepository accountRepository;
    StaffRepository staffRepository;
    BranchRepository branchRepository;
    StaffMapper staffMapper;
    PasswordEncoder passwordEncoder;

    private void isInfoExists(String username, String email, String phone) {
        if (accountRepository.findByUsername(username).isPresent()) throw new UsernameAlreadyExistsException();
        if (accountRepository.findByEmail(email).isPresent()) throw new EmailAlreadyExistsException();
        if (staffRepository.findByPhone(phone).isPresent()) throw new PhoneAlreadyExistsException();
    }

    private void normalizeRequest(StaffRequest request) {
        if (request.getUsername() != null) request.setUsername(request.getUsername().trim());
        if (request.getEmail() != null) request.setEmail(request.getEmail().trim().toLowerCase());
        if (request.getPhone() != null) request.setPhone(request.getPhone().trim());
        if (request.getFullName() != null) request.setFullName(request.getFullName().trim());
    }

    private void verifyManagerAccess(Staff targetStaff) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account managerAcc = accountRepository.findByUsername(auth.getName()).orElseThrow();
        if (managerAcc.getRole() == Role.MANAGER && managerAcc.getStaff().getBranch() != null) {
            Long managerBranchId = managerAcc.getStaff().getBranch().getBranchId();
            Long targetBranchId = targetStaff.getBranch() != null ? targetStaff.getBranch().getBranchId() : null;
            if (!managerBranchId.equals(targetBranchId)) {
                throw new AppException("BẢO MẬT: Bạn không có quyền thao tác trên nhân viên của chi nhánh khác!");
            }
        }
    }

    @Override
    @Transactional
    public void createManager(ManagerRequest request) {
        if (request.getBranchId() == null) {
            throw new AppException("Vui lòng chọn một chi nhánh chưa có người quản lý để phân công!");
        }

        if (staffRepository.existsByAccount_RoleAndBranch_BranchIdAndAccount_StatusNot(Role.MANAGER, request.getBranchId(), AccountStatus.DELETED)) {
            throw new AppException("Chi nhánh này đã có người quản lý.");
        }

        normalizeRequest(request);
        isInfoExists(request.getUsername(), request.getEmail(), request.getPhone());
        if (request.getPassword() == null || request.getPassword().isBlank()) throw new AppException("Mật khẩu không được để trống khi tạo mới!");
        if (request.getPassword().length() < 6) throw new AppException("Mật khẩu phải từ 6 đến 32 ký tự!");
        if (!request.getPassword().equals(request.getConfirmPassword())) throw new PasswordNotMatchException();

        Branch branch = branchRepository.findById(request.getBranchId()).orElseThrow(BranchNotFoundException::new);
        String passwordHash = passwordEncoder.encode(request.getPassword());

        Account account = staffMapper.toAccount(request);
        account.setRole(Role.MANAGER);
        account.setStatus(AccountStatus.ACTIVE);
        account.setPasswordHash(passwordHash);

        Staff staff = staffMapper.toStaff(request);
        staff.setBranch(branch);
        staff.setAccount(account);
        account.setStaff(staff);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void createStaff(StaffRequest request) {
        normalizeRequest(request);
        isInfoExists(request.getUsername(), request.getEmail(), request.getPhone());
        if (request.getPassword() == null || request.getPassword().isBlank()) throw new AppException("Mật khẩu không được để trống khi tạo mới!");
        if (request.getPassword().length() < 6) throw new AppException("Mật khẩu phải từ 6 đến 32 ký tự!");
        if (!request.getPassword().equals(request.getConfirmPassword())) throw new PasswordNotMatchException();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account managerAccount = accountRepository.findByUsername(auth.getName()).orElseThrow();
        if (managerAccount.getStaff() == null || managerAccount.getStaff().getBranch() == null) throw new AppException("Tài khoản của bạn chưa được phân công chi nhánh.");

        String passwordHash = passwordEncoder.encode(request.getPassword());
        Account account = staffMapper.toAccount(request);
        account.setRole(Role.STAFF);
        account.setStatus(AccountStatus.ACTIVE);
        account.setPasswordHash(passwordHash);

        Staff staff = staffMapper.toStaff(request);
        staff.setBranch(managerAccount.getStaff().getBranch());
        staff.setAccount(account);
        account.setStaff(staff);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void updateManager(Long staffId, ManagerRequest request) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(StaffNotFoundException::new);
        Account account = staff.getAccount();
        if (account.getStatus().equals(AccountStatus.DELETED)) throw new AccountDeletedException();

        Long currentBranchId = (staff.getBranch() == null) ? null : staff.getBranch().getBranchId();
        if (request.getBranchId() != null && !request.getBranchId().equals(currentBranchId) &&
                staffRepository.existsByAccount_RoleAndBranch_BranchIdAndAccount_StatusNot(Role.MANAGER, request.getBranchId(), AccountStatus.DELETED)) {
            throw new AppException("Chi nhánh này đã có người quản lý.");
        }
        normalizeRequest(request);
        if (!request.getUsername().equalsIgnoreCase(account.getUsername())) {
            if (accountRepository.existsByUsername(request.getUsername())) throw new UsernameAlreadyExistsException();
            account.setUsername(request.getUsername());
        }
        if (!request.getEmail().equalsIgnoreCase(account.getEmail())) {
            if (accountRepository.existsByEmail(request.getEmail())) throw new EmailAlreadyExistsException();
            account.setEmail(request.getEmail());
        }
        if (!request.getPhone().equals(staff.getPhone())) {
            if (staffRepository.findByPhone(request.getPhone()).isPresent()) throw new PhoneAlreadyExistsException();
            staff.setPhone(request.getPhone());
        }
        staff.setFullName(request.getFullName());

        Branch branch = null;
        if (request.getBranchId() != null) branch = branchRepository.findById(request.getBranchId()).orElseThrow(BranchNotFoundException::new);
        staff.setBranch(branch);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) throw new PasswordNotMatchException();
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void updateStaff(Long staffId, StaffRequest request) {
        normalizeRequest(request);
        Staff staff = staffRepository.findById(staffId).orElseThrow(StaffNotFoundException::new);
        Account account = staff.getAccount();
        if (account.getStatus().equals(AccountStatus.DELETED)) throw new AccountDeletedException();

        verifyManagerAccess(staff);

        if (!request.getUsername().equalsIgnoreCase(account.getUsername())) {
            if (accountRepository.existsByUsername(request.getUsername())) throw new UsernameAlreadyExistsException();
            account.setUsername(request.getUsername());
        }

        if (!request.getEmail().equalsIgnoreCase(account.getEmail())) {
            if (accountRepository.existsByEmail(request.getEmail())) throw new EmailAlreadyExistsException();
            account.setEmail(request.getEmail());
        }

        if (!request.getPhone().equals(staff.getPhone())) {
            if (staffRepository.findByPhone(request.getPhone()).isPresent()) throw new PhoneAlreadyExistsException();
            staff.setPhone(request.getPhone());
        }

        staff.setFullName(request.getFullName());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6 || request.getPassword().length() > 32) throw new AppException("Mật khẩu mới phải từ 6 đến 32 ký tự!");
            if (!request.getPassword().equals(request.getConfirmPassword())) throw new PasswordNotMatchException();
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteManager(Long staffId) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(StaffNotFoundException::new);
        staff.setBranch(null);
        Account account = staff.getAccount();
        if (!account.getEmail().contains("_deleted_")) {
            account.setEmail(account.getEmail() + "_deleted_" + System.currentTimeMillis());
        }
        account.setStatus(AccountStatus.DELETED);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(StaffNotFoundException::new);
        verifyManagerAccess(staff);

        Account account = staff.getAccount();
        if (!account.getEmail().contains("_deleted_")) {
            account.setEmail(account.getEmail() + "_deleted_" + System.currentTimeMillis());
        }
        account.setStatus(AccountStatus.DELETED);
        accountRepository.save(account);
    }
}