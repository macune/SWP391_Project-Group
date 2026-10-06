package fu.cinema.service.impl;

import fu.cinema.dto.request.MovieRequest;
import fu.cinema.dto.response.MovieResponse;
import fu.cinema.entity.Movie;
import fu.cinema.enums.MovieStatus;
import fu.cinema.exception.InvalidMovieDataException;
import fu.cinema.repository.MovieRepository;
import fu.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    private void validateMovieData(MovieRequest request, boolean isCreate) {
        if (request.getDuration() == null || request.getDuration() <= 0) {
            throw new InvalidMovieDataException("Lỗi: Thời lượng phim phải lớn hơn 0 phút.");
        }

        if (isCreate && request.getReleaseDate() != null && request.getReleaseDate().isBefore(LocalDate.now())) {
            throw new InvalidMovieDataException("Lỗi: Ngày khởi chiếu không được phép nằm trong quá khứ.");
        }
    }

    @Override
    public void addMovie(MovieRequest request) {
        validateMovieData(request, true);

        Movie movie = Movie.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .duration(request.getDuration())
                .releaseDate(request.getReleaseDate())
                .language(request.getLanguage())
                .genre(request.getGenre())
                .ageRating(request.getAgeRating())
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                .status(request.getStatus() != null ? request.getStatus() : MovieStatus.COMING_SOON)
                .build();

        movieRepository.save(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies(String keyword) {
        List<Movie> movies;

        if (keyword != null && !keyword.trim().isEmpty()) {
            movies = movieRepository.findByTitleContainingIgnoreCase(keyword.trim());
        } else {
            movies = movieRepository.findAll();
        }

        return movies.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + id));
        return toResponse(movie);
    }

    @Override
    public void updateMovie(Long id, MovieRequest request) {
        validateMovieData(request, false);

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + id));

        movie.setTitle(request.getTitle().trim());
        movie.setDescription(request.getDescription());
        movie.setDuration(request.getDuration());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setLanguage(request.getLanguage())
        ;
        movie.setGenre(request.getGenre());
        movie.setAgeRating(request.getAgeRating());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setTrailerUrl(request.getTrailerUrl());

        if (request.getStatus() != null) {
            movie.setStatus(request.getStatus());
        }

        movieRepository.save(movie);
    }

    @Override
    public void deleteMovie(Long id) {
        if (movieRepository.existsById(id)) {
            movieRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Không thể xóa. Không tìm thấy phim với ID: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkDuplicateTitle(String title) {
        return movieRepository.existsByTitleIgnoreCase(title != null ? title.trim() : "");
    }

    private MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
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
                .build();
    }
}