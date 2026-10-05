package fu.cinema.service;

import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Staff;
import fu.cinema.enums.BranchStatus;
import fu.cinema.enums.Role;
import fu.cinema.enums.StaffStatus;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;

    public BranchService(BranchRepository branchRepository,
                         AccountRepository accountRepository,
                         StaffRepository staffRepository) {
        this.branchRepository = branchRepository;
        this.accountRepository = accountRepository;
        this.staffRepository = staffRepository;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public List<Account> getEligibleUsersForManager() {
        return accountRepository.findByRole(Role.CUSTOMER);
    }

    @Transactional
    public Branch createBranch(Branch branch) {
        if (branchRepository.existsByBranchName(branch.getBranchName())) {
            throw new IllegalArgumentException("Tên chi nhánh đã tồn tại!");
        }
        branch.setStatus(BranchStatus.CLOSED);
        return branchRepository.save(branch);
    }

    @Transactional
    public void updateBranch(Branch updatedBranch) {
        Branch existing = branchRepository.findById(updatedBranch.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));

        if (!existing.getBranchName().equals(updatedBranch.getBranchName()) &&
                branchRepository.existsByBranchName(updatedBranch.getBranchName())) {
            throw new IllegalArgumentException("Tên chi nhánh đã tồn tại!");
        }

        existing.setBranchName(updatedBranch.getBranchName());
        existing.setAddress(updatedBranch.getAddress());
        existing.setHotline(updatedBranch.getHotline());
        existing.setOpeningTime(updatedBranch.getOpeningTime());
        existing.setClosingTime(updatedBranch.getClosingTime());
        existing.setStatus(updatedBranch.getStatus());

        branchRepository.save(existing);
    }

    @Transactional
    public void deleteBranch(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new IllegalArgumentException("Không tìm thấy chi nhánh để xóa!");
        }
        branchRepository.deleteById(branchId);
    }

    // Bổ nhiệm (hoặc thay thế) Manager duy nhất cho chi nhánh
    @Transactional
    public void assignManager(Long branchId, Long accountId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));
        Account newAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dùng!"));

        if (newAccount.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Chỉ có thể chọn người dùng bình thường (CUSTOMER) để bổ nhiệm làm Manager!");
        }

        // 1. Nếu chi nhánh đã có Manager cũ -> Thu hồi quyền, trả về CUSTOMER và xóa hồ sơ Staff cũ
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
        staffRepository.flush(); // Đồng bộ ngay với DB để dọn sạch Session

        // 2. Nâng quyền cho User mới
        newAccount.setRole(Role.MANAGER);

        Staff staff = newAccount.getStaff();
        if (staff == null) {
            staff = new Staff();
            staff.setAccount(newAccount);
            newAccount.setStaff(staff);
        }
        staff.setBranch(branch);
        staff.setEmail(newAccount.getEmail());
        staff.setStatus(StaffStatus.ACTIVE);

        if (newAccount.getCustomer() != null) {
            staff.setFullName(newAccount.getCustomer().getFullName());
            staff.setPhone(newAccount.getCustomer().getPhone());
        } else {
            staff.setFullName(newAccount.getUsername());
            staff.setPhone("Chưa cập nhật");
        }

        // Tự động lưu Staff thông qua Cascade của Account, tránh lỗi xung đột Session
        accountRepository.save(newAccount);
    }

    // Hủy quyền Quản lý của chi nhánh
    @Transactional
    public void removeManager(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));

        List<Staff> existingStaffList = staffRepository.findByBranchBranchId(branchId);
        boolean found = false;
        for (Staff s : existingStaffList) {
            if (s.getAccount() != null && s.getAccount().getRole() == Role.MANAGER) {
                Account oldAccount = s.getAccount();
                if (oldAccount != null) {
                    oldAccount.setRole(Role.CUSTOMER);
                    oldAccount.setStaff(null);
                    accountRepository.save(oldAccount);
                }
                staffRepository.delete(s);
                found = true;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Chi nhánh này hiện chưa có Manager để gỡ!");
        }
        staffRepository.flush();
    }
}