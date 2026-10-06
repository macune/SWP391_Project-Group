package fu.cinema.controller;

import fu.cinema.dto.request.MovieRequest;
import fu.cinema.dto.response.MovieResponse;
import fu.cinema.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public String listMovies(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("movies", movieService.getAllMovies(keyword));

        model.addAttribute("keyword", keyword);
        return "admin/list-movies";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("movieRequest")) {
            model.addAttribute("movieRequest", new MovieRequest());
        }
        return "admin/add-movie";
    }

    @PostMapping("/add")
    public String processAddMovie(@ModelAttribute("movieRequest") MovieRequest request,
                                  @RequestParam(required = false, name = "confirmDuplicate") Boolean confirmDuplicate,
                                  Model model) {
   
        if (!Boolean.TRUE.equals(confirmDuplicate) && movieService.checkDuplicateTitle(request.getTitle())) {
            model.addAttribute("duplicateWarning", "Phim '" + request.getTitle() + "' đã tồn tại trong hệ thống! Bạn có chắc chắn muốn thêm một bản ghi mới với tên trùng lặp không?");
            model.addAttribute("movieRequest", request);
            return "admin/add-movie";
        }

        movieService.addMovie(request);
        return "redirect:/movies?success";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        if (!model.containsAttribute("movieRequest")) {
            MovieResponse movie = movieService.getMovieById(id);
            MovieRequest request = MovieRequest.builder()
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
            model.addAttribute("movieRequest", request);
        }
        model.addAttribute("movieId", id);
        return "admin/edit-movie";
    }

    @PostMapping("/edit/{id}")
    public String processEditMovie(@PathVariable("id") Long id, @ModelAttribute("movieRequest") MovieRequest request) {
        movieService.updateMovie(id, request);
        return "redirect:/movies?success";
    }

    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable("id") Long id) {
        movieService.deleteMovie(id);
        return "redirect:/movies";
    }
}