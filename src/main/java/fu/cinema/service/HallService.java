package fu.cinema.service;

import fu.cinema.entity.Branch;
import fu.cinema.entity.Hall;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.HallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Hall createHall(Hall hall, Long branchId) {
        if (hallRepository.existsByBranchBranchIdAndHallName(branchId, hall.getHallName())) {
            throw new IllegalArgumentException("Tên phòng chiếu đã tồn tại trong chi nhánh này!");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh hợp lệ!"));

        hall.setBranch(branch);
        return hallRepository.save(hall);
    }

    @Transactional
    public void updateHall(Hall updatedHall, Long branchId) {
        Hall existing = hallRepository.findById(updatedHall.getHallId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu!"));

        if (!existing.getHallName().equals(updatedHall.getHallName()) &&
                hallRepository.existsByBranchBranchIdAndHallName(branchId, updatedHall.getHallName())) {
            throw new IllegalArgumentException("Tên phòng chiếu đã tồn tại!");
        }

        existing.setHallName(updatedHall.getHallName());
        existing.setCapacity(updatedHall.getCapacity());
        existing.setStatus(updatedHall.getStatus());

        hallRepository.save(existing);
    }

    @Transactional
    public void deleteHall(Long hallId) {
        if (!hallRepository.existsById(hallId)) {
            throw new IllegalArgumentException("Không tìm thấy phòng chiếu để xóa!");
        }
        hallRepository.deleteById(hallId);
    }
}