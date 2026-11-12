package fu.cinema.service;

import fu.cinema.dto.response.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboardData(Long branchId, String period);
}
