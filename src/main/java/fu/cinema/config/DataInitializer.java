package fu.cinema.config;

import fu.cinema.entity.Account;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Customer;
import fu.cinema.entity.Staff;
import fu.cinema.enums.AccountStatus;
import fu.cinema.enums.BranchStatus;
import fu.cinema.enums.Role;
import fu.cinema.repository.AccountRepository;
import fu.cinema.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Tạo Chi nhánh mẫu (branch_id = 1) nếu chưa có để test ManagerHallController
        Branch defaultBranch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch branch = Branch.builder()
                    .branchName("CineFlow Hà Nội")
                    .address("Cầu Giấy, Thành phố Hà Nội")
                    .hotline("0901234567")
                    .openingTime(LocalTime.of(8, 0))
                    .closingTime(LocalTime.of(23, 30))
                    .status(BranchStatus.ACTIVE)
                    .build();
            return branchRepository.save(branch);
        });

        String defaultPasswordHash = passwordEncoder.encode("123456");

        // 2. Tài khoản ADMIN
        if (!accountRepository.existsByUsername("admin")) {
            Account adminAcc = Account.builder()
                    .username("admin")
                    .email("admin@cineflow.vn")
                    .passwordHash(defaultPasswordHash)
                    .role(Role.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .build();

            Staff adminProfile = Staff.builder()
                    .account(adminAcc)
                    .branch(null)
                    .fullName("Quản Trị Viên Hệ Thống")
                    .phone("0900000001")
                    .build();

            adminAcc.setStaff(adminProfile);
            accountRepository.save(adminAcc);
        }

        // 3. Tài khoản MANAGER (Phụ trách Chi nhánh #1)
        if (!accountRepository.existsByUsername("manager")) {
            Account managerAcc = Account.builder()
                    .username("manager")
                    .email("manager@cineflow.vn")
                    .passwordHash(defaultPasswordHash)
                    .role(Role.MANAGER)
                    .status(AccountStatus.ACTIVE)
                    .build();

            Staff managerProfile = Staff.builder()
                    .account(managerAcc)
                    .branch(defaultBranch)
                    .fullName("Nguyễn Quản Lý Rạp")
                    .phone("0900000002")
                    .build();

            managerAcc.setStaff(managerProfile);
            accountRepository.save(managerAcc);
        }

        // 4. Tài khoản STAFF (Nhân viên POS tại Chi nhánh #1)
        if (!accountRepository.existsByUsername("staff")) {
            Account staffAcc = Account.builder()
                    .username("staff")
                    .email("staff@cineflow.vn")
                    .passwordHash(defaultPasswordHash)
                    .role(Role.STAFF)
                    .status(AccountStatus.ACTIVE)
                    .build();

            Staff staffProfile = Staff.builder()
                    .account(staffAcc)
                    .branch(defaultBranch)
                    .fullName("Trần Nhân Viên Quầy Vé")
                    .phone("0900000003")
                    .build();

            staffAcc.setStaff(staffProfile);
            accountRepository.save(staffAcc);
        }

        // 5. Tài khoản CUSTOMER (Khách hàng đã xác thực email)
        if (!accountRepository.existsByUsername("customer")) {
            Account customerAcc = Account.builder()
                    .username("customer")
                    .email("customer@cineflow.vn")
                    .passwordHash(defaultPasswordHash)
                    .role(Role.CUSTOMER)
                    .status(AccountStatus.ACTIVE)
                    .build();

            Customer customerProfile = Customer.builder()
                    .account(customerAcc)
                    .fullName("Lê Khách Hàng Thân Thiết")
                    .phone("0900000004")
                    .emailVerified(true)
                    .build();

            customerAcc.setCustomer(customerProfile);
            accountRepository.save(customerAcc);
        }

        log.info("=== Đã khởi tạo xong 4 tài khoản mẫu (admin, manager, staff, customer | pass: 123456) ===");
    }
}