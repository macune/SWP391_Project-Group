package fu.cinema.service.impl;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import fu.cinema.entity.Branch;
import fu.cinema.entity.BranchStock;
import fu.cinema.entity.FnBItem;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.BranchStockRepository;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchStockServiceImpl implements BranchStockService {

    private final BranchStockRepository branchStockRepository;
    private final BranchRepository branchRepository;
    private final FnBItemRepository fnBItemRepository;

    @Override
    public List<BranchStockResponse> getStockByBranch(Long branchId) {
        List<BranchStock> stocks = branchStockRepository.findByBranch_BranchId(branchId);

        return stocks.stream().map(stock -> BranchStockResponse.builder()
                .id(stock.getId())
                .branchId(stock.getBranch().getBranchId())
                .branchName(stock.getBranch().getBranchName())
                .itemId(stock.getFnbItem().getItemId())
                .itemName(stock.getFnbItem().getName())
                .itemCategory(stock.getFnbItem().getCategory().name())
                .quantity(stock.getQuantity())
                .lastUpdated(stock.getLastUpdated())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void importStock(BranchStockRequest request) {
        // Tìm xem món này ở chi nhánh này đã có bản ghi tồn kho chưa
        BranchStock stock = branchStockRepository
                .findByBranch_BranchIdAndFnbItem_ItemId(request.getBranchId(), request.getItemId())
                .orElse(null);

        if (stock != null) {
            // Nếu đã có trong kho -> Cộng dồn số lượng
            stock.setQuantity(stock.getQuantity() + request.getQuantityToAdd());
            branchStockRepository.save(stock);
        } else {
            // Nếu là món mới tinh chưa từng có ở chi nhánh này -> Tạo mới
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
            FnBItem item = fnBItemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món Bắp/Nước"));

            BranchStock newStock = BranchStock.builder()
                    .branch(branch)
                    .fnbItem(item)
                    .quantity(request.getQuantityToAdd())
                    .build();
            branchStockRepository.save(newStock);
        }
    }
}