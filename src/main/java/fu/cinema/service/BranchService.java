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

    // Lấy danh sách tài khoản người dùng bình thường để chọn làm Manager
    public List<Account> getEligibleUsersForManager() {
        return accountRepository.findByRole(Role.CUSTOMER);
    }

    @Transactional
    public Branch createBranch(Branch branch) {
        if (branchRepository.existsByBranchName(branch.getBranchName())) {
            throw new IllegalArgumentException("Tên chi nhánh đã tồn tại!");
        }

        // Trạng thái ban đầu của branch luôn là CLOSED
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

    // Thêm 1 user thông thường trở thành Manager của chi nhánh
    @Transactional
    public void assignManager(Long branchId, Long accountId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dùng!"));

        if (account.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Chỉ có thể chọn người dùng bình thường (CUSTOMER) để nâng quyền thành Manager!");
        }

        // Cập nhật vai trò tài khoản thành MANAGER
        account.setRole(Role.MANAGER);
        accountRepository.save(account);

        // Tạo hồ sơ nhân sự (Staff) gắn với chi nhánh
        Staff staff = staffRepository.findById(accountId).orElse(new Staff());
        staff.setStaffId(account.getAccountId());
        staff.setAccount(account);
        staff.setBranch(branch);
        staff.setEmail(account.getEmail());
        staff.setStatus(StaffStatus.ACTIVE);

        if (account.getCustomer() != null) {
            staff.setFullName(account.getCustomer().getFullName());
            staff.setPhone(account.getCustomer().getPhone());
        } else {
            staff.setFullName(account.getUsername());
            staff.setPhone("Chưa cập nhật");
        }

        staffRepository.save(staff);
    }

    // Xóa Manager khỏi chi nhánh và đưa tài khoản về người dùng thông thường
    @Transactional
    public void removeManager(Long branchId, Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ Manager!"));

        if (staff.getBranch() == null || !staff.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Manager này không thuộc chi nhánh hiện tại!");
        }

        Account account = staff.getAccount();
        if (account != null) {
            account.setRole(Role.CUSTOMER);
            accountRepository.save(account);
        }

        staffRepository.delete(staff);
    }
}