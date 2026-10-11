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
import java.util.stream.Collectors;

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

        List<MovieResponse> allShowingMovies = movieService.getMoviesByStatus("NOW_SHOWING");
        List<MovieResponse> filteredMovies = allShowingMovies.stream()
                .filter(m -> keyword == null || keyword.trim().isEmpty() || m.getTitle().toLowerCase().contains(keyword.toLowerCase().trim()))
                .filter(m -> genre == null || genre.isEmpty() || m.getGenre().equals(genre))
                .filter(m -> language == null || language.isEmpty() || m.getLanguage().equals(language))
                .filter(m -> ageRating == null || ageRating.isEmpty() || m.getAgeRating().equals(ageRating))
                .collect(Collectors.toList());

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
            @PathVariable("id") Long movieId,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "timeSlot", required = false) String timeSlot,
            Model model) {
        try {
            model.addAttribute("movie", movieService.getMovieDetail(movieId));
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("showtimes", movieService.getShowtimes(movieId, branchId, date, timeSlot));

            model.addAttribute("selectedBranchId", branchId);
            model.addAttribute("selectedDate", date);
            model.addAttribute("selectedTimeSlot", timeSlot);
        } catch (IllegalArgumentException e) {
            return "redirect:/customer/list-movies";
        }
        return "customer/movie-detail";
    }
}