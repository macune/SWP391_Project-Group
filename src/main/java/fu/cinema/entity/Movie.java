package fu.cinema.entity;

import fu.cinema.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "language", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String language;

    @Column(name = "genre", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String genre;

    @Column(name = "age_rating", nullable = false, length = 20)
    private String ageRating;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private MovieStatus status = MovieStatus.COMING_SOON;

    // Lưu ý: Đảm bảo bạn đã có class Showtime, nếu chưa hãy comment tạm 3 dòng này lại
    //@OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    //@Builder.Default
    //private List<Showtime> showtimes = new ArrayList<>();
}