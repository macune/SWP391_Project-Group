package fu.cinema.service.impl;

import fu.cinema.dto.request.BranchRequest;
import fu.cinema.dto.response.BranchResponse;
import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Staff;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.BranchStatus;
import fu.cinema.enums.Role;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.repository.*;
import fu.cinema.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;
    private final HallRepository hallRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private final BranchStockRepository branchStockRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getEligibleUsersForManager() {
        return accountRepository.findByRoleAndStatusNotAndStaff_BranchIsNull(Role.MANAGER, AccountStatus.DELETED);
    }

    @Override
    public BranchResponse createBranch(BranchRequest request) {
        String cleanName = request.getBranchName() != null ? request.getBranchName().trim() : "";
        String cleanHotline = request.getHotline() != null ? request.getHotline().trim() : "";
        String cleanAddress = request.getAddress() != null ? request.getAddress().trim() : "";

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Tên chi nhánh không được để trống!");
        }
        if (branchRepository.existsByBranchNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Tên chi nhánh '" + cleanName + "' đã tồn tại!");
        }
        if (!cleanHotline.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Số điện thoại (Hotline) phải gồm đúng 10 chữ số!");
        }
        if (branchRepository.existsByHotline(cleanHotline)) {
            throw new IllegalArgumentException("Số điện thoại '" + cleanHotline + "' đã được sử dụng cho chi nhánh khác!");
        }

        Branch branch = Branch.builder()
                .branchName(cleanName)
                .address(cleanAddress)
                .hotline(cleanHotline)
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

        String cleanName = request.getBranchName() != null ? request.getBranchName().trim() : "";
        String cleanHotline = request.getHotline() != null ? request.getHotline().trim() : "";
        String cleanAddress = request.getAddress() != null ? request.getAddress().trim() : "";

        if (branchRepository.existsByBranchNameIgnoreCaseAndBranchIdNot(cleanName, existing.getBranchId())) {
            throw new IllegalArgumentException("Tên chi nhánh '" + cleanName + "' đã tồn tại!");
        }
        if (!cleanHotline.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Số điện thoại (Hotline) phải gồm đúng 10 chữ số!");
        }
        if (branchRepository.existsByHotlineAndBranchIdNot(cleanHotline, existing.getBranchId())) {
            throw new IllegalArgumentException("Số điện thoại '" + cleanHotline + "' đã được sử dụng cho chi nhánh khác!");
        }

        if (request.getStatus() == BranchStatus.CLOSED && existing.getStatus() != BranchStatus.CLOSED) {
            LocalDateTime now = LocalDateTime.now();
            showtimeRepository.updateCompletedShowtimes(
                    now,
                    ShowtimeStatus.SCHEDULED,
                    ShowtimeStatus.SHOWING,
                    ShowtimeStatus.COMPLETED
            );

            boolean hasUnshownShowtimes = showtimeRepository.existsByHall_Branch_BranchIdAndStatusIn(
                    existing.getBranchId(),
                    List.of(ShowtimeStatus.SCHEDULED, ShowtimeStatus.SHOWING)
            );
            if (hasUnshownShowtimes) {
                throw new IllegalArgumentException("Không thể chuyển trạng thái sang CLOSED vì chi nhánh đang có suất chiếu chưa chiếu!");
            }
        }

        existing.setBranchName(cleanName);
        existing.setAddress(cleanAddress);
        existing.setHotline(cleanHotline);
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
        if (ticketRepository.existsByShowtime_Hall_Branch_BranchId(branchId)) {
            throw new IllegalArgumentException("Không thể xóa chi nhánh '" + existing.getBranchName() + "' vì đã có vé phát sinh thuộc chi nhánh này!");
        }
        if (showtimeRepository.existsByHall_Branch_BranchId(branchId)) {
            throw new IllegalArgumentException("Không thể xóa chi nhánh '" + existing.getBranchName() + "' vì đã có lịch chiếu (Showtime) thuộc chi nhánh này!");
        }
        if (hallRepository.existsByBranchBranchId(branchId)) {
            throw new IllegalArgumentException("Không thể xóa chi nhánh '" + existing.getBranchName() + "' vì đã có phòng chiếu (Hall) thuộc chi nhánh này!");
        }
        if (branchStockRepository.existsByBranch_BranchId(branchId)) {
            throw new IllegalArgumentException("Không thể xóa chi nhánh '" + existing.getBranchName() + "' vì đã có tồn kho F&B (BranchStock) thuộc chi nhánh này!");
        }
        if (staffRepository.existsByBranchBranchId(branchId)) {
            throw new IllegalArgumentException("Không thể xóa chi nhánh '" + existing.getBranchName() + "' vì đang có nhân sự (Staff / Manager) gắn với chi nhánh này!");
        }
        branchRepository.delete(existing);
    }

    @Override
    public void assignManager(Long branchId, Long accountId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh!"));
        Account newAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản Quản lý!"));

        if (newAccount.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException("Chỉ có thể chọn tài khoản có vai trò Quản lý (MANAGER)!");
        }

        Staff newManagerStaff = newAccount.getStaff();
        if (newManagerStaff != null && newManagerStaff.getBranch() != null) {
            throw new IllegalArgumentException("Quản lý này đã được phân công cho một chi nhánh khác!");
        }

        List<Staff> existingStaffList = staffRepository.findByBranchBranchId(branchId);
        for (Staff s : existingStaffList) {
            if (s.getAccount() != null && s.getAccount().getRole() == Role.MANAGER) {
                s.setBranch(null);
                staffRepository.save(s);
            }
        }
        staffRepository.flush();

        if (newManagerStaff == null) {
            throw new IllegalArgumentException("Tài khoản Quản lý này chưa có hồ sơ nhân sự (Staff)!");
        }
        newManagerStaff.setBranch(branch);
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
                s.setBranch(null);
                staffRepository.save(s);
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