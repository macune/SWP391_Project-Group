package fu.cinema.repository;

import fu.cinema.entity.Account;
import fu.cinema.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByAccount(Account account);
    void deleteByAccount(Account account);
}
