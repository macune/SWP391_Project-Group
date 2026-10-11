package fu.cinema.controller;

import fu.cinema.service.BranchService;
import fu.cinema.service.PublicMovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PublicMovieController {

    private final PublicMovieService movieService;
    private final BranchService branchService;

    public PublicMovieController(PublicMovieService movieService, BranchService branchService) {
        this.movieService = movieService;
        this.branchService = branchService;
    }

    @GetMapping({"/", "/movies"})
    public String listMovies(@RequestParam(value = "search", required = false) String keyword, Model model) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("movies", movieService.searchMovies(keyword));
            model.addAttribute("keyword", keyword);
            model.addAttribute("pageTitle", "Kết quả tìm kiếm cho: " + keyword);
        } else {
            model.addAttribute("movies", movieService.getMoviesByStatus("NOW_SHOWING"));
            model.addAttribute("comingSoonMovies", movieService.getMoviesByStatus("COMING_SOON"));
            model.addAttribute("pageTitle", "Phim Đang Chiếu");
        }
        return "public/movie-list";
    }

    @GetMapping("/movies/{id}")
    public String movieDetail(@PathVariable("id") Long movieId,
                              @RequestParam(value = "branchId", required = false) Long branchId,
                              Model model) {
        try {
            model.addAttribute("movie", movieService.getMovieDetail(movieId));
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("showtimes", movieService.getShowtimes(movieId, branchId));
            model.addAttribute("selectedBranchId", branchId);
        } catch (IllegalArgumentException e) {
            return "redirect:/movies"; // Nếu phim không tồn tại thì quay về trang chủ
        }
        return "public/movie-detail";
    }
}