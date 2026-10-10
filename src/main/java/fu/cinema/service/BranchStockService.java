package fu.cinema.service;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import java.util.List;

public interface BranchStockService {
    //Lấy danh sách tồn kho của 1 chi nhánh
    List<BranchStockResponse> getStockByBranch(Long branchId);

    //Nhập thêm hàng vào kho
    void importStock(BranchStockRequest request);
}