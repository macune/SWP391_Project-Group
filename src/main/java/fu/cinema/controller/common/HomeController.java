package fu.cinema.controller.common;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.enums.AgeRating;
import fu.cinema.enums.Genre;
import fu.cinema.enums.Language;
import fu.cinema.service.PublicMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PublicMovieService movieService;

    @GetMapping({"/", "/home"})
    public String publicHome(
            @RequestParam(value = "search", required = false) String keyword,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "ageRating", required = false) String ageRating,
            Model model) {

        model.addAttribute("genres", Genre.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("ageRatings", AgeRating.values());

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

        if ((keyword != null && !keyword.trim().isEmpty()) || (genre != null && !genre.isEmpty()) ||
                (language != null && !language.isEmpty()) || (ageRating != null && !ageRating.isEmpty())) {
            model.addAttribute("pageTitle", "Kết quả tìm kiếm");
        } else {
            model.addAttribute("comingSoonMovies", movieService.getMoviesByStatus("COMING_SOON"));
            model.addAttribute("pageTitle", "Phim Đang Chiếu Tại CineFlow");
        }

        return "common/home";
    }

    @GetMapping("/movie-detail/{id}")
    public String movieDetail(@PathVariable("id") Long movieId, Model model) {
        try {
            MovieResponse movie = movieService.getMovieDetail(movieId);
            model.addAttribute("movie", movie);
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
        return "common/movie-detail";
    }
}