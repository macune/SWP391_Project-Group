package fu.cinema.service;

import fu.cinema.dto.request.HallRequest;
import fu.cinema.dto.request.SeatUpdateItem;
import fu.cinema.dto.response.HallResponse;
import fu.cinema.dto.response.SeatResponse;
import fu.cinema.entity.Branch;

import java.util.List;
import java.util.Map;

public interface HallService {

    Branch getBranchById(Long branchId);

    List<HallResponse> getHallsByBranch(Long branchId);

    HallResponse getHallById(Long hallId);

    HallResponse createHallWithSeats(Long branchId, HallRequest request);

    HallResponse updateHall(Long branchId, HallRequest request);

    void deleteHall(Long hallId);

    Map<String, List<SeatResponse>> getSeatMapByHall(Long hallId);

    void updateSeatMatrix(Long hallId, List<SeatUpdateItem> seatUpdates);
}