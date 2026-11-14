package fu.cinema.dto.response;

import org.springframework.data.domain.Page;
import java.util.Map;

public record AdminFnbItemTransactionsResponse(
        Page<FnbRevenueRowResponse> rows,
        Map<Long, String> transactionBranchNames
) {
}
