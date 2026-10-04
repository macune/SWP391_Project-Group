package fu.cinema.service;

import fu.cinema.entity.Branch;
import fu.cinema.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Branch createBranch(Branch branch) {
        if (branchRepository.existsByBranchName(branch.getBranchName())) {
            throw new IllegalArgumentException("Tên chi nhánh đã tồn tại!");
        }
        return branchRepository.save(branch);
    }
}