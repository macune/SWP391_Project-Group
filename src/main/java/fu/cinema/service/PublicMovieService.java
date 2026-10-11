package fu.cinema.service;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.dto.response.ShowtimeResponse;

import java.util.List;

public interface PublicMovieService {
    List<MovieResponse> getMoviesByStatus(String statusName);
    List<MovieResponse> searchMovies(String keyword);
    MovieResponse getMovieDetail(Long movieId);
    List<ShowtimeResponse> getShowtimes(Long movieId, Long branchId);
}