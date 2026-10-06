package fu.cinema.dto.request;

import fu.cinema.enums.MovieStatus;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRequest {
    private String title;
    private String description;
    private Integer duration;
    private LocalDate releaseDate;
    private String language;
    private String genre;
    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private MovieStatus status;
}