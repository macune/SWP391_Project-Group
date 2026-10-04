package fu.cinema.service;

import fu.cinema.dto.request.MovieRequest;
import fu.cinema.dto.response.MovieResponse;
import fu.cinema.entity.Movie;
import fu.cinema.enums.MovieStatus;
import fu.cinema.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MovieService {
    @Autowired
    private MovieRepository movieRepository;

    // 1.Thêm phim mới
    public void addMovie(MovieRequest request) {
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(request.getDuration())
                .releaseDate(request.getReleaseDate())
                .language(request.getLanguage())
                .genre(request.getGenre())
                .ageRating(request.getAgeRating())
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                // Gán mặc định là COMING_SOON nếu người dùng không chọn
                .status(request.getStatus() != null ? request.getStatus() : MovieStatus.COMING_SOON)
                .build();

        movieRepository.save(movie);
    }

    // 2.Lấy danh sách phim
    public List<MovieResponse> getAllMovies() {
        List<Movie> movies = movieRepository.findAll();
        List<MovieResponse> responses = new ArrayList<>();

        for (Movie movie : movies) {
            responses.add(MovieResponse.builder()
                    .movieId(movie.getMovieId())
                    .title(movie.getTitle())
                    .description(movie.getDescription())
                    .duration(movie.getDuration())
                    .releaseDate(movie.getReleaseDate())
                    .language(movie.getLanguage())
                    .genre(movie.getGenre())
                    .ageRating(movie.getAgeRating())
                    .posterUrl(movie.getPosterUrl())
                    .trailerUrl(movie.getTrailerUrl())
                    .status(movie.getStatus())
                    .build());
        }
        return responses;
    }

    // 3.Xóa phim
    public void deleteMovie(Long id) {
        // Kiểm tra xem phim có tồn tại không trước khi xóa
        if (movieRepository.existsById(id)) {
            movieRepository.deleteById(id);
        }
    }
}