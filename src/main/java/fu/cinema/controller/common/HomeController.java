package fu.cinema.controller.common;

import fu.cinema.service.BranchService;
import fu.cinema.service.PublicMovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final PublicMovieService movieService;
    private final BranchService branchService;

    // Inject Service thông qua constructor
    public HomeController(PublicMovieService movieService, BranchService branchService) {
        this.movieService = movieService;
        this.branchService = branchService;
    }

    // 1. Trang chủ dùng chung cho Guest và toàn hệ thống (Hiển thị list phim)
    @GetMapping({"/", "/home"})
    public String publicHome(@RequestParam(value = "search", required = false) String keyword, Model model) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("movies", movieService.searchMovies(keyword));
            model.addAttribute("keyword", keyword);
            model.addAttribute("pageTitle", "Kết quả tìm kiếm cho: " + keyword);
        } else {
            model.addAttribute("movies", movieService.getMoviesByStatus("NOW_SHOWING"));
            model.addAttribute("comingSoonMovies", movieService.getMoviesByStatus("COMING_SOON"));
            model.addAttribute("pageTitle", "Phim Đang Chiếu Tại CineFlow");
        }

        return "common/home";
    }

    // 2. Trang chi tiết phim
    @GetMapping("/movie-detail/{id}")
    public String movieDetail(@PathVariable("id") Long movieId,
                              @RequestParam(value = "branchId", required = false) Long branchId,
                              Model model) {
        try {
            model.addAttribute("movie", movieService.getMovieDetail(movieId));
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("showtimes", movieService.getShowtimes(movieId, branchId));
            model.addAttribute("selectedBranchId", branchId);
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }

        return "common/movie-detail";
    }
}