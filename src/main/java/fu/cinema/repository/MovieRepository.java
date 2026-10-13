package fu.cinema.repository;

import fu.cinema.entity.Movie;
import fu.cinema.enums.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    boolean existsByTitleIgnoreCase(String title);
    List<Movie> findByTitleContainingIgnoreCase(String keyword);
    List<Movie> findByStatus(MovieStatus status);
}