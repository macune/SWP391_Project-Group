package fu.cinema.service;

import fu.cinema.dto.request.MovieRequest;
import fu.cinema.dto.response.MovieResponse;

import java.util.List;

public interface MovieService {

    void addMovie(MovieRequest request);

    List<MovieResponse> getAllMovies(String keyword);

    MovieResponse getMovieById(Long id);

    void updateMovie(Long id, MovieRequest request);

    void deleteMovie(Long id);

    boolean checkDuplicateTitle(String title);
}