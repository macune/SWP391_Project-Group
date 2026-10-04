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

    // 1. Mở trang Danh sách
    @GetMapping
    public String listMovies(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        return "admin/list-movies";
    }

    // 2. Mở form Thêm phim
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("movieRequest", new MovieRequest());
        return "admin/add-movie";
    }

    // 3. Lưu Phim
    @PostMapping("/add")
    public String processAddMovie(@ModelAttribute("movieRequest") MovieRequest request) {
        movieService.addMovie(request);
        return "redirect:/movies?success";
    }

    // 4. Xóa phim
    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable("id") Long id) {
        movieService.deleteMovie(id);
        return "redirect:/movies";
    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id) {
        return "redirect:/movies";
    }
}