package fu.cinema.repository;

import fu.cinema.dto.response.CustomerResponse;
import fu.cinema.entity.Customer;
import fu.cinema.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);

    List<Customer> findByEmailVerifiedAndAccount_StatusNot(Boolean isEmailVerified, AccountStatus status);
}
