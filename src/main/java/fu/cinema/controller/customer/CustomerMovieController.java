package fu.cinema.controller.customer;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.enums.AgeRating;
import fu.cinema.enums.Genre;
import fu.cinema.enums.Language;
import fu.cinema.service.BranchService;
import fu.cinema.service.PublicMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerMovieController {

    private final PublicMovieService movieService;
    private final BranchService branchService;

    @GetMapping("/list-movies")
    public String customerListMovies(
            @RequestParam(value = "search", required = false) String keyword,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "ageRating", required = false) String ageRating,
            Model model) {

        model.addAttribute("genres", Genre.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("ageRatings", AgeRating.values());
        model.addAttribute("topMovies", movieService.getTop5HighestRevenueMovies());

        // Gọi thẳng Service xử lý lọc thay vì tự xử lý Stream trong Controller
        List<MovieResponse> filteredMovies = movieService.getFilteredMovies(
                "NOW_SHOWING", keyword, genre, language, ageRating);

        model.addAttribute("movies", filteredMovies);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("selectedLanguage", language);
        model.addAttribute("selectedAgeRating", ageRating);

        model.addAttribute("comingSoonMovies", movieService.getMoviesByStatus("COMING_SOON"));
        model.addAttribute("pageTitle", "Phim Đang Chiếu Tại CineFlow");

        return "customer/list-movies";
    }

    @GetMapping("/movie-detail/{id}")
    public String customerMovieDetail(
            @PathVariable Long id,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "timeSlot", required = false) String timeSlot,
            Model model) {

        if (date == null) {
            date = LocalDate.now();
        }

        model.addAttribute("movie", movieService.getMovieDetail(id));
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("showtimes", movieService.getShowtimes(id, branchId, date, timeSlot));

        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedTimeSlot", timeSlot);

        return "customer/movie-detail";
    }
}