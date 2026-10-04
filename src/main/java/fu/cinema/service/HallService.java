package fu.cinema.service;

import fu.cinema.entity.Branch;
import fu.cinema.entity.Hall;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.HallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallService {

    private final HallRepository hallRepository;
    private final BranchRepository branchRepository;

    public HallService(HallRepository hallRepository, BranchRepository branchRepository) {
        this.hallRepository = hallRepository;
        this.branchRepository = branchRepository;
    }

    public List<Hall> getHallsByBranch(Long branchId) {
        return hallRepository.findByBranchBranchId(branchId);
    }

    public Hall createHall(Hall hall, Long branchId) {
        if (hallRepository.existsByBranchBranchIdAndHallName(branchId, hall.getHallName())) {
            throw new IllegalArgumentException("Tên phòng chiếu đã tồn tại trong chi nhánh này!");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh hợp lệ!"));

        hall.setBranch(branch);
        return hallRepository.save(hall);
    }
}