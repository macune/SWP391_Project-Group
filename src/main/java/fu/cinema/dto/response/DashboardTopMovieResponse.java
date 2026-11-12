package fu.cinema.dto.response;

public record DashboardTopMovieResponse(
        Long movieId,
        String title,
        String posterUrl,
        long tickets
) {
}
