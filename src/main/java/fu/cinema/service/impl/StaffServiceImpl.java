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
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }
        if (accountRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException();
        }
        if (staffRepository.findByPhone(phone).isPresent()) {
            throw new PhoneAlreadyExistsException();
        }
    }

    private void normalizeRequest(StaffRequest request) {
        if (request.getUsername() != null) {
            request.setUsername(request.getUsername().trim());
        }
        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhone() != null) {
            request.setPhone(request.getPhone().trim());
        }
        if (request.getFullName() != null) {
            request.setFullName(request.getFullName().trim());
        }
    }

    @Override
    @Transactional
    public void createManager(ManagerRequest request) {
        // Kiểm tra chi nhánh đã có quản lý chưa (chỉ kiểm tra khi Admin có chọn chi nhánh)
        if (request.getBranchId() != null &&
                staffRepository.existsByAccount_RoleAndBranch_BranchIdAndAccount_StatusNot(Role.MANAGER, request.getBranchId(), AccountStatus.DELETED)) {
            throw new AppException("Chi nhánh này đã có người quản lý.");
        }

        normalizeRequest(request);
        isInfoExists(request.getUsername(), request.getEmail(), request.getPhone());

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AppException("Mật khẩu không được để trống khi tạo mới!");
        }
        if (request.getPassword().length() < 6) {
            throw new AppException("Mật khẩu phải từ 6 đến 32 ký tự!");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordNotMatchException();
        }

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(BranchNotFoundException::new);
        }

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

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AppException("Mật khẩu không được để trống khi tạo mới!");
        }
        if (request.getPassword().length() < 6) {
            throw new AppException("Mật khẩu phải từ 6 đến 32 ký tự!");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordNotMatchException();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException("Vui lòng đăng nhập để thực hiện thao tác này.");
        }
        String currentUser = auth.getName();
        Account managerAccount = accountRepository.findByUsername(currentUser)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin tài khoản đang đăng nhập!"));

        if (managerAccount.getStaff() == null || managerAccount.getStaff().getBranch() == null) {
            throw new AppException("Tài khoản quản lý của bạn chưa được phân công chi nhánh.");
        }

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
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(StaffNotFoundException::new);
        Account account = staff.getAccount();

        // Chặn cập nhật nếu tài khoản quản lý đã bị xóa mềm
        if (account.getStatus().equals(AccountStatus.DELETED)) {
            throw new AccountDeletedException();
        }

        // Lấy chi nhánh hiện tại của Manager (nếu chưa gán chi nhánh thì currentBranchId = null)
        Long currentBranchId = (staff.getBranch() == null) ? null : staff.getBranch().getBranchId();

        // Kiểm tra chi nhánh: chỉ kiểm tra khi Admin có chọn chi nhánh (request.getBranchId() != null)
        // và chi nhánh được chọn KHÁC với chi nhánh hiện tại của chính Manager này
        if (request.getBranchId() != null && !request.getBranchId().equals(currentBranchId) &&
                staffRepository.existsByAccount_RoleAndBranch_BranchIdAndAccount_StatusNot(Role.MANAGER, request.getBranchId(), AccountStatus.DELETED)) {
            throw new AppException("Chi nhánh này đã có người quản lý.");
        }

        normalizeRequest(request);

        // 1. Kiểm tra Username: chỉ check DB khi có thay đổi
        if (!request.getUsername().equalsIgnoreCase(account.getUsername())) {
            if (accountRepository.existsByUsername(request.getUsername())) {
                throw new UsernameAlreadyExistsException();
            }
            account.setUsername(request.getUsername());
        }

        // 2. Kiểm tra Email: chỉ check DB khi có thay đổi
        if (!request.getEmail().equalsIgnoreCase(account.getEmail())) {
            if (accountRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException();
            }
            account.setEmail(request.getEmail());
        }

        // 3. Kiểm tra Số điện thoại: chỉ check DB khi có thay đổi
        if (!request.getPhone().equals(staff.getPhone())) {
            if (staffRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new PhoneAlreadyExistsException();
            }
            staff.setPhone(request.getPhone());
        }

        // 4. Cập nhật Họ và tên
        staff.setFullName(request.getFullName());

        // 5. Cập nhật Chi nhánh phụ trách
        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(BranchNotFoundException::new);
        }
        staff.setBranch(branch);

        // 6. Cập nhật mật khẩu tùy chọn (chỉ đổi nếu nhập mật khẩu mới)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6 || request.getPassword().length() > 32) {
                throw new AppException("Mật khẩu mới phải từ 6 đến 32 ký tự!");
            }
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new PasswordNotMatchException();
            }
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void updateStaff(Long staffId, StaffRequest request) {
        normalizeRequest(request);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(StaffNotFoundException::new);
        Account account = staff.getAccount();

        if (staff.getAccount().getStatus().equals(AccountStatus.DELETED)) {
            throw new AccountDeletedException();
        }

        // 1. Kiểm tra Username: chỉ check DB khi có thay đổi
        if (!request.getUsername().equalsIgnoreCase(account.getUsername())) {
            if (accountRepository.existsByUsername(request.getUsername())) {
                throw new UsernameAlreadyExistsException();
            }
            account.setUsername(request.getUsername());
        }

        // 2. Kiểm tra Email: chỉ check DB khi có thay đổi
        if (!request.getEmail().equalsIgnoreCase(account.getEmail())) {
            if (accountRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException();
            }
            account.setEmail(request.getEmail());
        }

        // 3. Kiểm tra Số điện thoại: chỉ check DB khi có thay đổi
        if (!request.getPhone().equals(staff.getPhone())) {
            if (staffRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new PhoneAlreadyExistsException();
            }
            staff.setPhone(request.getPhone());
        }

        // 4. Cập nhật Họ và tên
        staff.setFullName(request.getFullName());

        // 5. Cập nhật mật khẩu tùy chọn (chỉ đổi nếu nhập mật khẩu mới)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6 || request.getPassword().length() > 32) {
                throw new AppException("Mật khẩu mới phải từ 6 đến 32 ký tự!");
            }
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new PasswordNotMatchException();
            }
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        accountRepository.save(account);
    }

    // Xóa mềm Quản lý: gỡ rạp phụ trách và chuyển trạng thái sang DELETED
    @Override
    @Transactional
    public void deleteManager(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(StaffNotFoundException::new);
        staff.setBranch(null); // Gỡ khỏi chi nhánh để chi nhánh trống chỗ bổ nhiệm người mới
        Account account = staff.getAccount();
        account.setStatus(AccountStatus.DELETED);
        accountRepository.save(account);
    }

    // Xóa mềm Nhân viên: chuyển trạng thái sang DELETED
    @Override
    @Transactional
    public void deleteStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(StaffNotFoundException::new);
        Account account = staff.getAccount();
        account.setStatus(AccountStatus.DELETED);
        accountRepository.save(account);
    }
}
