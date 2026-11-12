package fu.cinema.dto.response;

import org.springframework.data.domain.Page;
import java.util.Map;

public record AdminShowtimeRevenueResponse(
        Page<RevenueShowtimeResponse> showtimes,
        Map<Long, String> showtimeBranchNames
) {
}
