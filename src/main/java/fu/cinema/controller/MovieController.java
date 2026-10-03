package fu.cinema.controller;

import fu.cinema.dto.request.MovieRequest;
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
    public String listMovies(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        return "list-movies";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("movieRequest", new MovieRequest());
        return "add-movie";
    }

    @PostMapping("/add")
    public String processAddMovie(@ModelAttribute("movieRequest") MovieRequest request) {
        movieService.addMovie(request);
        return "redirect:/movies?success";
    }
}