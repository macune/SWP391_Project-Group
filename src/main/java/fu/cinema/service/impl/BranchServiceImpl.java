package fu.cinema.service.impl;

import fu.cinema.dto.request.BranchRequest;
import fu.cinema.dto.response.BranchResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Staff;
import fu.cinema.enums.BranchStatus;
import fu.cinema.enums.Role;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.StaffRepository;
import fu.cinema.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getEligibleUsersForManager() {
        return accountRepository.findByRole(Role.CUSTOMER);
    }

    @Override
    public BranchResponse createBranch(BranchRequest request) {
        String cleanName = request.getBranchName().trim();
        if (branchRepository.existsByBranchName(cleanName)) {
            throw new IllegalArgumentException("Tên chi nhánh '" + cleanName + "' đã tồn tại!");
        }

        Branch branch = Branch.builder()
                .branchName(cleanName)
                .address(request.getAddress().trim())
                .hotline(request.getHotline().trim())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .status(request.getStatus() != null ? request.getStatus() : BranchStatus.ACTIVE)
                .build();

        return toResponse(branchRepository.save(branch));
    }

    @Override
    public BranchResponse updateBranch(BranchRequest request) {
        Branch existing = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));

        String cleanName = request.getBranchName().trim();
        if (!existing.getBranchName().equalsIgnoreCase(cleanName) &&
                branchRepository.existsByBranchName(cleanName)) {
            throw new IllegalArgumentException("Tên chi nhánh '" + cleanName + "' đã tồn tại!");
        }

        existing.setBranchName(cleanName);
        existing.setAddress(request.getAddress().trim());
        existing.setHotline(request.getHotline().trim());
        existing.setOpeningTime(request.getOpeningTime());
        existing.setClosingTime(request.getClosingTime());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        return toResponse(branchRepository.save(existing));
    }

    @Override
    public void deleteBranch(Long branchId) {
        Branch existing = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh để xóa!"));

        // Nếu còn nhân sự đang gán vào chi nhánh thì gỡ chi nhánh hoặc thu hồi Manager trước khi xóa
        List<Staff> staffInBranch = staffRepository.findByBranchBranchId(branchId);
        for (Staff s : staffInBranch) {
            if (s.getAccount() != null && s.getAccount().getRole() == Role.MANAGER) {
                Account acc = s.getAccount();
                acc.setRole(Role.CUSTOMER);
                acc.setStaff(null);
                accountRepository.save(acc);
                staffRepository.delete(s);
            } else {
                s.setBranch(null);
                staffRepository.save(s);
            }
        }
        staffRepository.flush();
        branchRepository.delete(existing);
    }

    @Override
    public void assignManager(Long branchId, Long accountId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));
        Account newAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dùng!"));

        if (newAccount.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Chỉ có thể chọn tài khoản Khách hàng (CUSTOMER) để bổ nhiệm làm Manager!");
        }

        // 1. Nếu chi nhánh đã có Manager cũ -> Thu hồi quyền về CUSTOMER và xóa hồ sơ Staff cũ
        List<Staff> existingStaffList = staffRepository.findByBranchBranchId(branchId);
        for (Staff s : existingStaffList) {
            if (s.getAccount() != null && s.getAccount().getRole() == Role.MANAGER) {
                Account oldAccount = s.getAccount();
                oldAccount.setRole(Role.CUSTOMER);
                oldAccount.setStaff(null);
                accountRepository.save(oldAccount);
                staffRepository.delete(s);
            }
        }
        staffRepository.flush();

        // 2. Nâng quyền cho User mới (Không còn setEmail và setStatus trên Staff)
        newAccount.setRole(Role.MANAGER);

        Staff staff = newAccount.getStaff();
        if (staff == null) {
            staff = new Staff();
            staff.setAccount(newAccount);
            newAccount.setStaff(staff);
        }
        staff.setBranch(branch);

        if (newAccount.getCustomer() != null) {
            staff.setFullName(newAccount.getCustomer().getFullName());
            staff.setPhone(newAccount.getCustomer().getPhone());
        } else {
            staff.setFullName(newAccount.getUsername());
            staff.setPhone("0900000000");
        }

        accountRepository.save(newAccount);
    }

    @Override
    public void removeManager(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new IllegalArgumentException("Không tìm thấy chi nhánh!");
        }

        List<Staff> existingStaffList = staffRepository.findByBranchBranchId(branchId);
        boolean found = false;
        for (Staff s : existingStaffList) {
            if (s.getAccount() != null && s.getAccount().getRole() == Role.MANAGER) {
                Account oldAccount = s.getAccount();
                oldAccount.setRole(Role.CUSTOMER);
                oldAccount.setStaff(null);
                accountRepository.save(oldAccount);
                staffRepository.delete(s);
                found = true;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Chi nhánh này hiện chưa có Manager để gỡ!");
        }
        staffRepository.flush();
    }

    private BranchResponse toResponse(Branch branch) {
        Staff manager = branch.getManager();
        Account managerAcc = (manager != null) ? manager.getAccount() : null;

        return BranchResponse.builder()
                .branchId(branch.getBranchId())
                .branchName(branch.getBranchName())
                .address(branch.getAddress())
                .hotline(branch.getHotline())
                .openingTime(branch.getOpeningTime())
                .closingTime(branch.getClosingTime())
                .status(branch.getStatus())
                .managerId(manager != null ? manager.getStaffId() : null)
                .managerFullName(manager != null ? manager.getFullName() : null)
                .managerUsername(managerAcc != null ? managerAcc.getUsername() : null)
                .managerEmail(managerAcc != null ? managerAcc.getEmail() : null)
                .managerPhone(manager != null ? manager.getPhone() : null)
                .build();
    }
}