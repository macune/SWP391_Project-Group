package fu.cinema.repository;

import fu.cinema.entity.Movie;
import fu.cinema.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    boolean existsByTitleIgnoreCase(String title);
    List<Movie> findByTitleContainingIgnoreCase(String keyword);
    List<Movie> findByStatus(MovieStatus status);

    @Query(value = """
        SELECT m FROM Movie m
        WHERE (:status IS NULL OR m.status = :status)
          AND (:movieName IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :movieName, '%')))
        """, countQuery = """
        SELECT COUNT(m) FROM Movie m
        WHERE (:status IS NULL OR m.status = :status)
          AND (:movieName IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :movieName, '%')))
        """)
    Page<Movie> findMoviesForRevenueReport(
            @Param("status") MovieStatus status,
            @Param("movieName") String movieName,
            Pageable pageable
    );
}